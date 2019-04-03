package com.hik.base

import java.util.concurrent.Executors
import com.hik.base.main.IDRunable
import com.hik.base.util.{CETCProtocol, ConfigUtil}
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.{SparkConf, SparkContext}
/**
 * * 说明：
  * * 此项目为ID虚拟身份采集记录
  * * 数据分别存储于Redis和Hbase中
  * * 在Hbase中存放Mac采集记录表名分别为
  * * IDRecoder:原始记录，以采集设备ID为key
  * * IDRecoder_dateInx：以时间为key
  * * IDRecoder_macInx：以Mac为key
  * * MTIDInfo：采集到的Mac列表
  * * 在Redis中存放 (注意过期时间)
  * * key：Total_IDRecoder  value:mac采集总记录数
  * * key：DayTotal_IDRecoderYYYY(年份) value:每年采集总记录数
  * * key：DayTotal_IDRecoderYYYYMM(月份) value:每月采集总记录数
  * * key：DayTotal_IDRecoderYYYYMMDD(日) value:每日采集总记录数
  * * key：devmin_2019_id_14306073X00037F23F8E3 value:单设备每年采集总记录数
  * * key：devmin_201902_id_14306073X00037F23F8E3 value:单设备每月采集总记录数
  * * key：devmin_20190219_id_14306073X00037F23F8E3 value:单设备每日采集总记录数
  * * key：devmin_2019021913_id_14306073X00037F23F8E3 value:单设备每时采集总记录数
  * * key：devmin_201902191324_id_14306073X00037F23F8E3 value:单设备每分采集总记录数
 */
object App {
  val IDRECORDER_CONF:String="dataAnalysis/idrecoder.xml"
  val HBASE_SITE_CONF:String="hbase/hbase-site.xml"
  val APP_NAME="IDDataRecorder"
  def main(args:Array[String]):Unit={
    //val path="E:\\BaseDataRecoder\\conf\\"
    val path=args(0)
    val conf = new SparkConf().setAppName(APP_NAME)
    //conf.setMaster("local")
    val sc=new SparkContext(conf)
    val executors = Executors.newCachedThreadPool()
    val idssc=new StreamingContext(sc,Seconds(3))
    val g2016IDconf = new ConfigUtil( path+IDRECORDER_CONF)
    g2016IDconf.setConfPath(path+HBASE_SITE_CONF);
    val G2016IDTopic:IDRunable= new IDRunable(idssc,g2016IDconf,sc)
    executors.submit(G2016IDTopic)
  }
}
