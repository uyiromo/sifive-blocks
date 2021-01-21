package sifive.blocks.devices.nvmmctr

import chisel3._
import chisel3.core.dontTouch
import chisel3.experimental.chiselName
import chisel3.util._
import chisel3.experimental.{IntParam, BaseModule}
import freechips.rocketchip.amba.axi4._
import freechips.rocketchip.subsystem.{BaseSubsystem, PeripheryBusKey}
import freechips.rocketchip.config.{Parameters, Field, Config}
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.regmapper.{HasRegMap, RegField}
import freechips.rocketchip.tilelink._
import freechips.rocketchip.util.UIntIsOneOf

case object PeripheryNVMMCTRKey extends Field[Option[NVMMCTRParams]](None)

/*
class WithNVMMCTR(address: BigInt, size: BigInt = 4096) extends Config((site, here, up)
{
  case PeripheryNVMMCTRKey => Some(NVMMCTRParams(address, size))
})
 */



trait HasPeripheryNVMMCTR { this: BaseSubsystem =>
  private val portName = "nvmmctr"
  private val params = p(PeripheryNVMMCTRKey).get

  val nvmmctr = LazyModule(new TLNVMMCTR(params, pbus.beatBytes)(p))
  pbus.toVariableWidthSlave(Some(portName)) { nvmmctr.node }

}

trait HasPeripheryNVMMCTRModuleImp extends LazyModuleImp
{
  val outer: HasPeripheryNVMMCTR

  val nvmmctr_io = IO(new PeripheryNVMMCTRIO())
  nvmmctr_io <> outer.nvmmctr.module.io
  dontTouch(nvmmctr_io)
}
