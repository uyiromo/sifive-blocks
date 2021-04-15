package sifive.blocks.devices.nvmmctr

import chisel3._
import chisel3.core.dontTouch
import chisel3.experimental.chiselName
import chisel3.util._
import chisel3.experimental.{IntParam, BaseModule}
import freechips.rocketchip.amba.axi4._
import freechips.rocketchip.subsystem.BaseSubsystem
import freechips.rocketchip.config.{Parameters, Field, Config}
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.regmapper.{HasRegMap, RegField}
import freechips.rocketchip.tilelink._
import freechips.rocketchip.util.UIntIsOneOf

/**
  * @file   NVMMCTR.scala
  * @brief  NVMMConTRol module for Freedom U500 platform
  * @author Yu OMORI
  * @date   2020/06/17
  */


/**
  * Parameter for NVMMCTR instantiation
  *
  * @constructor
  *
  * @param address [required] address of NVMMCTR in CPU address map
  * @param size    [option]   size of NVMMCTR in CPU address map
  *                           NVMMCTR will occupy [address : (address+size)]
  */
@chiselName
case class NVMMCTRParams(
  address: BigInt,
  size:    BigInt  = 4096
)


/**
  * To hold constant for NVMMCTR instantiation
  *
  * reg_width  : width of MMIO registers in [bits]
  * addr_width : width of araddr/awaddr in [bits]
  * lat_width  : width of latency register
  * mem_size   : total size of memory (1-GiB)
  *
  * @constructor
  *
  */
object NVMMCTRConstant {
  val reg_width  = 64
  val bank_width = 3
  val low_width  = 10
  val addr_width = 32
  val lat_width  = 8
  val mem_size   = 0x40000000.U
  val end_addr   = 0x3fffffff.U
}



/************************************
 *                                  *
 *             I/O Port             *
 *                                  *
 ************************************/

/**
  * I/O for TLNVMMCTR
  * mbus <--> TLNVMMCTR <--> mig
  *
  * @contructor creare new I/O ports
  *
  * @param None
  *
 */
trait NVMMCTRIO extends Bundle
{
  // alias for constants
  val w = NVMMCTRConstant.reg_width
  val b = NVMMCTRConstant.bank_width
  val a = NVMMCTRConstant.addr_width
  val l = NVMMCTRConstant.lat_width

  //val clear      = Output(Bool())
  val nvmm_begin = Output(UInt(b.W))
  val lat_cr     = Output(UInt(l.W))
  val lat_cw     = Output(UInt(l.W))
  val lat_tRCD2  = Output(UInt(l.W))
  val lat_tRP2   = Output(UInt(l.W))
  val lat_tRAS2  = Output(UInt(11.W))
  val lat_dr256  = Output(UInt(l.W))
  val lat_dr4096 = Output(UInt(l.W))
  val lat_dw256  = Output(UInt(l.W))
  val lat_dw4096 = Output(UInt(l.W))
  val cnt_read   = Input(UInt(w.W))
  val cnt_write  = Input(UInt(w.W))
  val cnt_act    = Input(UInt(w.W))
  //val cnt_pre    = Input(UInt(w.W))
  val cnt_bdr    = Input(UInt(w.W))
  val cnt_bdw    = Input(UInt(w.W))
}


/**
  * I/O for NVMMCTR
  * mbus <--> TLNVMMCTR (NVMMCTR) <--> mig
  *
  * @contructor creare new I/O ports
  *
  * @param None
  *
 */
@chiselName
class PeripheryNVMMCTRIO extends NVMMCTRIO


/**
  * I/O for INNER module
  * NVMMCTR <--> NVMMCTRModule
  *
  * @contructor create new I/O ports
  *
  * @param None
  *
 */
@chiselName
class NVMMCTRModuleIO extends Bundle
{
  // alias for constants
  val w = NVMMCTRConstant.reg_width
  val b = NVMMCTRConstant.bank_width
  val a = NVMMCTRConstant.addr_width
  val l = NVMMCTRConstant.lat_width

  // basic
  //val clock = Input(Clock())
  //val reset = Input(Bool())

  // PeripheryBus <--> NVMMCTR
  val mbus_arvalid = Input(Bool())
  val mbus_arready = Output(Bool())
  val mbus_araddr  = Input(UInt(a.W))

  val mbus_awvalid = Input(Bool())
  val mbus_awready = Output(Bool())
  val mbus_awaddr  = Input(UInt(a.W))

  // NVMMCTR <--> MIG
  val mig_arvalid = Output(Bool())
  val mig_arready = Input(Bool())

  val mig_awvalid = Output(Bool())
  val mig_awready = Input(Bool())

  // Wire for MMIO registers
  //val clear      = Input(Bool())
  val nvmm_begin = Input(UInt(b.W))
  val lat_cr     = Input(UInt(l.W))
  val lat_cw     = Input(UInt(l.W))
  val lat_dr256  = Input(UInt(l.W))
  val lat_dr4096 = Input(UInt(l.W))
  val lat_dw256  = Input(UInt(l.W))
  val lat_dw4096 = Input(UInt(l.W))
  val cnt_read   = Output(UInt(w.W))
  val cnt_write  = Output(UInt(w.W))
  val cnt_bdr    = Output(UInt(w.W))
  val cnt_bdw    = Output(UInt(w.W))
}


/**
  * I/O for AX channel delay module
  * NVMMCTR <--> NVMMCTRModule
  *
  * @contructor create new I/O ports
  *
  * @param None
  *
 */
@chiselName
class NVMMCTRModuleAXIO extends Bundle
{
  // alias for constants
  val w = NVMMCTRConstant.reg_width
  val b = NVMMCTRConstant.bank_width
  val a = NVMMCTRConstant.addr_width
  val l = NVMMCTRConstant.lat_width

  // basic
  //val clock = Input(Clock())
  //val reset = Input(Bool())

  //val nvmm_begin = Input(UInt(b.W))

  // AX input
  val mbus_axvalid = Input(Bool())
  val mbus_axready = Output(Bool())
  //val mbus_axaddr  = Input(UInt(a.W))
  val mbus_axbank  = Input(UInt(b.W))
  val mbus_axlow12 = Input(UInt(12.W))

  // AX Output
  val mig_axvalid = Output(Bool())
  val mig_axready = Input(Bool())
  //val axaddr_i  = Input(UInt(a.W))

  /* latency */
  val lat_cx     = Input(UInt(l.W))
  val lat_dx256  = Input(UInt(l.W))
  val lat_dx4096 = Input(UInt(l.W))

  /* counter */
  val cnt_x      = Output(UInt(w.W))
  val cnt_bdx    = Output(UInt(w.W))

  val to_nvmm    = Input(Bool())
}





/************************************
 *                                  *
 *             Modules              *
 *                                  *
 ************************************/
/**
  * NVMMCTR inner AXI delay module
  *
  * @contructor
  *
  * @param None
  *
 */
@chiselName
class NVMMCTRModuleAX extends Module
{
  //implicit val p: Parameters
  //def params: NVMMCTRParams

  // alias for constants
  val w = NVMMCTRConstant.reg_width
  val b = NVMMCTRConstant.bank_width
  //val a = NVMMCTRConstant.addr_width
  val l = NVMMCTRConstant.lat_width

  val ZERO_B          = 0.U(b.W)
  val ZERO_L          = 0.U(l.W)
  val ZERO_W          = 0.U(w.W)
  val ZERO_12         = 0.U(12.W)
  val ONE_L           = 1.U(l.W)
  val ONE_W           = 1.U(w.W)
  val TWO_L           = 2.U(l.W)
  val B255            = 255.U(12.W)
  val B4096           = 4095.U(12.W)
  val TRUE            = true.B
  val FALSE           = false.B


  val io = IO(new NVMMCTRModuleAXIO())

  val cnt_ax   = RegInit(ONE_L)  /* number of delayed cycles from axvalid assertion */
  val ready_ax = RegInit(FALSE)  /* cnt_ax === lat, is latency is satisfied ot not */
  val busy_ax  = RegInit(FALSE)  /* is cnt_ax is busy or not */

  val cnt_x    = RegInit(ZERO_W)  /* counter of handshake to NVMM */
  val cnt_bdx  = RegInit(ZERO_W)  /* counter of bank_diff_x */

  val nz_lat_cx     = Wire(Bool())  /* lat_cx     is Non-Zero */
  val nz_lat_dx256  = Wire(Bool())  /* lat_dx256  is Non-Zero */
  val nz_lat_dx4096 = Wire(Bool())  /* lat_dx4096 is Non-Zero */

  //val to_nvmm       = Wire(Bool())
  val last_axbank   = RegInit(ZERO_B)  /* last_axaddr  */

  val do_delay_cx     = Wire(Bool())
  val do_delay_dx256  = Wire(Bool())
  val do_delay_dx4096 = Wire(Bool())

  val dont_delay      = Wire(Bool())

  /* wire assign */
  nz_lat_cx     := (io.lat_cx     > TWO_L)
  nz_lat_dx256  := (io.lat_dx256  > TWO_L)
  nz_lat_dx4096 := (io.lat_dx4096 > TWO_L)

  //to_nvmm       := (io.mbus_axbank >= io.nvmm_begin)

  do_delay_cx     := (nz_lat_cx)
  do_delay_dx256  := (nz_lat_dx256)  & ((io.mbus_axlow12 & B255)  === ZERO_12)
  do_delay_dx4096 := (nz_lat_dx4096) & ((io.mbus_axlow12 & B4096) === ZERO_12)

  dont_delay     := (!io.to_nvmm) | (!nz_lat_cx & !nz_lat_dx256 & !nz_lat_dx4096)

  io.mig_axvalid  := (dont_delay | ready_ax) & io.mbus_axvalid
  io.mbus_axready := (dont_delay | ready_ax) & io.mig_axready

  io.cnt_x        := cnt_x
  io.cnt_bdx      := cnt_bdx


  when (!dont_delay & !busy_ax & !ready_ax & io.mbus_axvalid) {
    // when this module is idle & new requests comes
    busy_ax := TRUE

    when (do_delay_dx4096) {
      cnt_ax := io.lat_dx4096
    } .elsewhen (do_delay_dx256) {
      cnt_ax := io.lat_dx256
    } .elsewhen (do_delay_cx) {
      cnt_ax := io.lat_cx
    }
  } .elsewhen (busy_ax) {
    // when busy
    cnt_ax := cnt_ax - ONE_L
    ready_ax := cnt_ax === TWO_L
    busy_ax  := cnt_ax =/= TWO_L
  //} .elsewhen (io.mig_axvalid & io.mig_axready) {
  } .otherwise {
    // deassert ready_ax
    ready_ax := FALSE
    //cnt_ax   := ONE_L
  }

  when (io.to_nvmm & !busy_ax & !ready_ax & io.mbus_axvalid) {
    cnt_x := cnt_x + ONE_W
    last_axbank := io.mbus_axbank

    when (last_axbank =/= io.mbus_axbank) {
      cnt_bdx := cnt_bdx + ONE_W
    }
  }

  /* disable DCE on firrtl */
  dontTouch(io)
}



/**
  * NVMMCTR inner module
  *
  * @contructor
  *
  * @param None
  *
 */
@chiselName
class NVMMCTRModule extends Module
{
  //implicit val p: Parameters
  //def params: NVMMCTRParams

  // alias for constants
  val w = NVMMCTRConstant.reg_width
  val l = NVMMCTRConstant.lat_width
  val b = NVMMCTRConstant.bank_width
  val a = NVMMCTRConstant.addr_width

  val io = IO(new NVMMCTRModuleIO())

  /* axdelay */
  val ar = Module(new NVMMCTRModuleAX())
  val aw = Module(new NVMMCTRModuleAX())

  // buffer for timing constraints
  val nvmm_begin = Reg(UInt(b.W))
  val lat_cr     = Reg(UInt(l.W))
  val lat_cw     = Reg(UInt(l.W))
  val lat_dr256  = Reg(UInt(l.W))
  val lat_dr4096 = Reg(UInt(l.W))
  val lat_dw256  = Reg(UInt(l.W))
  val lat_dw4096 = Reg(UInt(l.W))
  val cnt_read   = Reg(UInt(w.W))
  val cnt_write  = Reg(UInt(w.W))
  val cnt_bdr    = Reg(UInt(w.W))
  val cnt_bdw    = Reg(UInt(w.W))

  /*
  val lat_cr     = Wire(UInt(l.W))
  val lat_cw     = Wire(UInt(l.W))
  val lat_dr256  = Wire(UInt(l.W))
  val lat_dr4096 = Wire(UInt(l.W))
  val lat_dw256  = Wire(UInt(l.W))
  val lat_dw4096 = Wire(UInt(l.W))
   */
  nvmm_begin   := io.nvmm_begin
  lat_cr       := io.lat_cr
  lat_cw       := io.lat_cw
  lat_dr256    := io.lat_dr256
  lat_dr4096   := io.lat_dr4096
  lat_dw256    := io.lat_dw256
  lat_dw4096   := io.lat_dw4096
  io.cnt_read  := cnt_read
  io.cnt_write := cnt_write
  io.cnt_bdr   := cnt_bdr
  io.cnt_bdw   := cnt_bdw


  //ar.io.clock        := io.clock
  //ar.io.reset        := io.reset
  //ar.io.nvmm_begin   := nvmm_begin
  ar.io.mbus_axvalid := io.mbus_arvalid
  io.mbus_arready    := ar.io.mbus_axready
  //ar.io.mbus_axaddr  := io.mbus_araddr
  ar.io.to_nvmm      := (io.mbus_araddr(31,29) >= nvmm_begin)
  ar.io.mbus_axbank  := io.mbus_araddr(31,29)
  ar.io.mbus_axlow12 := io.mbus_araddr(11,0)
  io.mig_arvalid     := ar.io.mig_axvalid
  ar.io.mig_axready  := io.mig_arready
  ar.io.lat_cx       := lat_cr
  ar.io.lat_dx256    := lat_dr256
  ar.io.lat_dx4096   := lat_dr4096
  cnt_read           := ar.io.cnt_x
  cnt_bdr            := ar.io.cnt_bdx

  //aw.io.clock        := io.clock
  //aw.io.reset        := io.reset
  //aw.io.nvmm_begin   := nvmm_begin
  aw.io.mbus_axvalid := io.mbus_awvalid
  io.mbus_awready    := aw.io.mbus_axready
  //aw.io.mbus_axaddr  := io.mbus_awaddr
  aw.io.to_nvmm      := (io.mbus_awaddr(31,29) >= nvmm_begin)
  aw.io.mbus_axbank  := io.mbus_awaddr(31,29)
  aw.io.mbus_axlow12 := io.mbus_awaddr(11,0)
  io.mig_awvalid     := aw.io.mig_axvalid
  aw.io.mig_axready  := io.mig_awready
  aw.io.lat_cx       := lat_cw
  aw.io.lat_dx256    := lat_dw256
  aw.io.lat_dx4096   := lat_dw4096
  cnt_write          := aw.io.cnt_x
  cnt_bdw            := aw.io.cnt_bdx

  /* disable DCE on firrtl */
  dontTouch(io)
}



/**
  * NVMMCTR outer module
  *
  * @contructor
  *
  * @param None
  *
 */
trait NVMMCTR extends HasRegMap
{
  implicit val p: Parameters
  def params: NVMMCTRParams

  // alias for constants
  val w = NVMMCTRConstant.reg_width
  val b = NVMMCTRConstant.bank_width
  val a = NVMMCTRConstant.addr_width
  val l = NVMMCTRConstant.lat_width
  val m = NVMMCTRConstant.mem_size
  val e = NVMMCTRConstant.end_addr

  val clock: Clock
  val io: NVMMCTRIO

  /* wire between RegMap <--> NVMMCTRModule */
  //val clear      = Wire(new DecoupledIO(UInt(w.W)))
  //val clear      = Reg(UInt(w.W))
  val nvmm_begin = RegInit(0.U(b.W))
  val lat_cr     = RegInit(0.U(l.W))
  val lat_cw     = RegInit(0.U(l.W))
  val lat_tRCD2  = RegInit(0.U(l.W))
  val lat_tRP2   = RegInit(0.U(l.W))
  val lat_tRAS2  = RegInit(0.U(11.W))
  val lat_dr256  = RegInit(0.U(l.W))
  val lat_dr4096 = RegInit(0.U(l.W))
  val lat_dw256  = RegInit(0.U(l.W))
  val lat_dw4096 = RegInit(0.U(l.W))
  val cnt_read   = Reg(UInt(w.W))
  val cnt_write  = Reg(UInt(w.W))
  val cnt_act    = Reg(UInt(w.W))
  //val cnt_pre    = Reg(UInt(w.W))
  val cnt_bdr    = Reg(UInt(w.W))
  val cnt_bdw    = Reg(UInt(w.W))

  //io.clear      := clear
  io.nvmm_begin := nvmm_begin
  io.lat_cr     := lat_cr
  io.lat_cw     := lat_cw
  io.lat_tRCD2  := lat_tRCD2
  io.lat_tRP2   := lat_tRP2
  io.lat_tRAS2  := lat_tRAS2
  io.lat_dr256  := lat_dr256
  io.lat_dr4096 := lat_dr4096
  io.lat_dw256  := lat_dw256
  io.lat_dw4096 := lat_dw4096
  cnt_read      := io.cnt_read
  cnt_write     := io.cnt_write
  cnt_act       := io.cnt_act
  //cnt_pre       := io.cnt_act
  cnt_bdr       := io.cnt_bdr
  cnt_bdw       := io.cnt_bdw



  /* register map */
  regmap(
    //0x00 -> Seq(RegField.w(1, clear)),
    0x08 -> Seq(RegField(b, nvmm_begin)),
    0x10 -> Seq(RegField(l, lat_cr)),
    0x18 -> Seq(RegField(l, lat_cw)),
    0x20 -> Seq(RegField(l, lat_tRCD2)),
    0x28 -> Seq(RegField(l, lat_tRP2)),
    0x30 -> Seq(RegField(l, lat_tRAS2)),
    0x38 -> Seq(RegField(l, lat_dr256)),
    0x40 -> Seq(RegField(l, lat_dr4096)),
    0x48 -> Seq(RegField(l, lat_dw256)),
    0x50 -> Seq(RegField(l, lat_dw4096)),
    0x58 -> Seq(RegField.r(w, cnt_read)),
    0x60 -> Seq(RegField.r(w, cnt_write)),
    0x68 -> Seq(RegField.r(w, cnt_act)),
    //0x70 -> Seq(RegField.r(w, cnt_pre)),
    0x78 -> Seq(RegField.r(w, cnt_bdr)),
    0x80 -> Seq(RegField.r(w, cnt_bdw))
  )
}


/**
  * NVMMCTR module on TileLink
  *
  * @contructor
  *
  * @param None
  *
 */
@chiselName
class TLNVMMCTR(params: NVMMCTRParams, beatBytes: Int)(implicit p: Parameters)
    extends TLRegisterRouter(
  params.address, "nvmmctr", Seq("iromo,nvmmctr"), beatBytes = beatBytes)(
  new TLRegBundle(params, _) with NVMMCTRIO)(
  new TLRegModule(params, _, _) with NVMMCTR)
