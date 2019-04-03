package com.hik.base.main

import java.util.concurrent.Callable

import com.google.gson.Gson
import com.hik.base.bean.IDRecorder
import com.hik.base.util.{CETCProtocol, CommFunUtils, ConfigUtil}
import com.hiklife.utils.{HBaseUtil, RedisUtil}
import net.sf.json.JSONObject
import org.apache.hadoop.hbase.TableName
import org.apache.hadoop.hbase.client.{ConnectionFactory, HTable, Put}
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.dstream.InputDStream
import org.apache.spark.streaming.kafka010.ConsumerStrategies.Subscribe
import org.apache.spark.streaming.kafka010.LocationStrategies.PreferConsistent
import org.apache.spark.streaming.kafka010.{HasOffsetRanges, KafkaUtils, OffsetRange}
import org.apache.spark.{SparkContext, TaskContext}

class IDRunable(_ssc:StreamingContext, _configUtil:ConfigUtil, _sc:SparkContext) extends Callable[Unit] {

  val ssc = _ssc
  val configUtil = _configUtil
  val sc = _sc
  val hBaseUtil = new HBaseUtil(_configUtil.confPath)

  override def call(): Unit = {

    val topic = Array(configUtil.topic)

    val kafkaParams = Map[String, Object](
      "bootstrap.servers" -> configUtil.brokers,
      "key.deserializer" -> classOf[StringDeserializer],
      "value.deserializer" -> classOf[StringDeserializer],
      "group.id" -> configUtil.group,
      "auto.offset.reset" -> configUtil.offset,
      "enable.auto.commit" -> (false: java.lang.Boolean)
    )

    val broadList = sc.broadcast(List(_configUtil.confPath, configUtil.recoderTable,configUtil.recoderDateInx,configUtil.recoderMacInx,configUtil.recoderDuplicate, configUtil.redisHost, configUtil.redisPort, configUtil.redisTimeout, configUtil.kafkaOffsetKey))

      hBaseUtil.createTable(configUtil.recoderTable, "RD")
      hBaseUtil.createTable(configUtil.recoderDateInx, "RD")
      hBaseUtil.createTable(configUtil.recoderMacInx, "RD")
      hBaseUtil.createTable(configUtil.recoderDuplicate, "S")

      val stream=getStream(configUtil,topic,kafkaParams)
      stream.foreachRDD(rdd => {
        val offsetRanges = rdd.asInstanceOf[HasOffsetRanges].offsetRanges

        rdd.foreachPartition(partitionOfRecords => {
          //为每个分区新建Redis工具
          val redisUtil = new RedisUtil(broadList.value(5).toString, broadList.value(6).toString.toInt, broadList.value(7).toString.toInt)
          val conn = ConnectionFactory.createConnection(HBaseUtil.getConfiguration(broadList.value(0).asInstanceOf[String]))
          val devtable = conn.getTable(TableName.valueOf(broadList.value(1).asInstanceOf[String])).asInstanceOf[HTable]
          val datetable = conn.getTable(TableName.valueOf(broadList.value(2).asInstanceOf[String])).asInstanceOf[HTable]
          val mactable = conn.getTable(TableName.valueOf(broadList.value(3).asInstanceOf[String])).asInstanceOf[HTable]
          val duplicatetable = conn.getTable(TableName.valueOf(broadList.value(4).asInstanceOf[String])).asInstanceOf[HTable]

          devtable.setAutoFlush(false, false)
          devtable.setWriteBufferSize(5 * 1024 * 1024)
          datetable.setAutoFlush(false, false)
          datetable.setWriteBufferSize(5 * 1024 * 1024)
          mactable.setAutoFlush(false, false)
          mactable.setWriteBufferSize(5 * 1024 * 1024)
          duplicatetable.setAutoFlush(false, false)
          duplicatetable.setWriteBufferSize(5 * 1024 * 1024)
          redisUtil.connect()
          partitionOfRecords.foreach(record => {
            val records = record.value().split("\t",-1)
            if (records.length == 19) {
              val m: IDRecorder = new IDRecorder;
              m.ToIDRecoder(records);
              //以devId为分类
              var rowkey_dev=CommFunUtils.getIDRecoderByDevRowkey(m)
              //以时间为分类
              var rowkey_date=CommFunUtils.getIDRecoderByDateRowkey(m)
              //以mac为分类
              var rowkey_mac=CommFunUtils.getIDRowkey(m)
              //值
              val value = (new Gson).toJson(m, classOf[IDRecorder])
              //按设备ID存放记录
              CommFunUtils.putValue(devtable, rowkey_dev, value)
              //按日期存放记录
              CommFunUtils.putValue(datetable, rowkey_date, rowkey_dev)
              //按ID存放记录
              CommFunUtils.putValue(mactable, rowkey_mac, rowkey_dev)
              //按ID存放记录
              CommFunUtils.putDupValue(duplicatetable, m.getTy+"_"+m.getId, m)
              //统计总条数
              redisUtil.jedis.incr("Total_IDRecoder")
              //每天的统计量
              CommFunUtils.putGroupDevIds(redisUtil,"DayTotal_IDRecoder"+CommFunUtils.getNowDate)
              CommFunUtils.putGroupDevIds(redisUtil,"DayTotal_IDRecoder_Type"+CommFunUtils.getNowDate+"_"+m.getTy)
              //每月的统计量
              CommFunUtils.putGroupDevIds(redisUtil,"DayTotal_IDRecoder"+CommFunUtils.getMonthNowDate)
              CommFunUtils.putGroupDevIds(redisUtil,"DayTotal_IDRecoder_Type"+CommFunUtils.getMonthNowDate+"_"+m.getTy)
              //每年的统计量
              CommFunUtils.putGroupDevIds(redisUtil,"DayTotal_IDRecoder"+CommFunUtils.getYearNowDate)
              CommFunUtils.putGroupDevIds(redisUtil,"DayTotal_IDRecoder_Type"+CommFunUtils.getYearNowDate+"_"+m.getTy)
              //按devId分类来统计总记条数
              CommFunUtils.putGroupDevId(redisUtil, m,2*3600,CommFunUtils.getMinNowDate())
              CommFunUtils.putGroupDevId(redisUtil, m,2*3600,CommFunUtils.getHourNowDate())
              CommFunUtils.putGroupDevId(redisUtil, m,2*24*3600,CommFunUtils.getNowDate())
              CommFunUtils.putGroupDevId(redisUtil, m,32*24*3600,CommFunUtils.getMonthNowDate())
              CommFunUtils.putGroupDevId(redisUtil, m,2*365*24*3600,CommFunUtils.getYearNowDate())
            }//按epc查
          })
          redisUtil.close()
          devtable.flushCommits()
          devtable.close()
          datetable.flushCommits()
          datetable.close()
          mactable.flushCommits()
          mactable.close()
          duplicatetable.flushCommits()
          duplicatetable.close()
          conn.close()
                  //记录本次消费offset
          val o: OffsetRange = offsetRanges(TaskContext.get.partitionId)
          val key = s"${o.topic}_${o.partition}"
          val kafkaOffsetKey = broadList.value(8).toString
          var isRun = false
          while (!isRun){
            isRun = CETCProtocol.setOffset(redisUtil,kafkaOffsetKey,key,o.fromOffset.toString)
          }
        })
      })
      ssc.start()
      ssc.awaitTermination()
    }





  def getStream(configUtil: ConfigUtil, topic:Array[String], kafkaParams:Map[String,Object]):InputDStream[ConsumerRecord[String, String]]={
    //从redis中获取kafka上次消费偏移量
    val redisUtil = new RedisUtil(configUtil.redisHost, configUtil.redisPort, configUtil.redisTimeout)
    val offsetVal = redisUtil.getObject(configUtil.kafkaOffsetKey)
    var stream = if (offsetVal == null || offsetVal == None) {
      //从最新开始消费
      KafkaUtils.createDirectStream[String, String](ssc, PreferConsistent, Subscribe[String, String](topic, kafkaParams))
    } else {
      //从上次消费偏移位置开始消费
      var fromOffsets: Map[TopicPartition, Long] = Map()
      val map = offsetVal.asInstanceOf[Map[String, String]]
      for (result <- map) {
        val nor = result._1.split("_")
        val tp = new TopicPartition(nor(0), nor(1).toInt)
        fromOffsets += (tp -> result._2.toLong)
      }
      KafkaUtils.createDirectStream[String,String](ssc, PreferConsistent,Subscribe[String,String](topic,kafkaParams,fromOffsets))
    }
    stream
  }


}
