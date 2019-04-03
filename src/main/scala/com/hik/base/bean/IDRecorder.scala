package com.hik.base.bean

import java.text.SimpleDateFormat

import com.hik.base.util.{CETCProtocol, CommFunUtils}

class IDRecorder {
  /**
    * 终端mac
    */
  private var mac = ""
  def getMac:String=mac
  /**
    * 终端品牌
    */
  private var bd = ""

  /**
    * 进入时间（采集时间）
    */
  private var ct = ""
  def getCt:String=ct
  /**
    * 虚拟身份类型
    */
  private var ty = ""
  def getTy:String=ty
  /**
    * 虚拟身份内容
    */
  private var id = ""
  def getId:String=id
  /**
    * 终端场强
    */
  private var rssi = ""

  /**
    * 当前连接ssid
    */
  private var ssid = ""

  /**
    * 热点mac
    */
  private var apmac = ""

  /**
    * 热点通道
    */
  private var apc = ""

  /**
    * 热点加密类型
    */
  private var ape = ""

  /**
    * x坐标
    */
  private var x = ""

  /**
    * y坐标
    */
  private var y = ""

  /**
    * 场所编号
    */
  private var nw = ""

  /**
    * 设备编号
    */
  private var devID = ""
  def getDevId:String=devID
  /**
    * 经度
    */
  private var lg = ""

  /**
    * 维度
    */
  private var la = ""
  /**
    * 历史ssid
    */
  private var cssid = ""

  /**
    *是否进入
    */
  private var isenter=""

  def ToIDRecoder(array: Array[String]): Unit = {
    mac = CETCProtocol.Escape(array(0)) //38-29-5A-0D-9B-29
    bd = CETCProtocol.Escape(array(1)) //GUANGDONG OPPO MOBILE TELECOMMUNICATIONS CORP.,LTD
    cssid=CETCProtocol.Escape(array(2))
    ct = CETCProtocol.GetStrDate(CETCProtocol.IntParse(CETCProtocol.Escape(array(3)))) //1548384668
    rssi = CETCProtocol.Escape(array(4)) //-63
    ty=CETCProtocol.Escape(array(5))
    id=CETCProtocol.Escape(array(6))
    ssid = CETCProtocol.Escape(array(7)) //CMCC-NuxA
    apmac=CETCProtocol.Escape(array(8))
    apc = CETCProtocol.Escape(array(9)) //5
    ape = CETCProtocol.Escape(array(10)) //2
    x = CETCProtocol.Escape(array(11)) //0
    y = CETCProtocol.Escape(array(12)) //0
    nw = CETCProtocol.Escape(array(13)) //42082139000003
    devID = CETCProtocol.Escape(array(14)) //14306073X00037F8364FB
    lg = CETCProtocol.Escape(array(15)) //113.12968
    la = CETCProtocol.Escape(array(16)) //31.02796
    isenter = CETCProtocol.Escape(array(17)) //1*/
    //用于辅助判断进出入时间
    val at=CETCProtocol.Escape(array(18))
  }

}
