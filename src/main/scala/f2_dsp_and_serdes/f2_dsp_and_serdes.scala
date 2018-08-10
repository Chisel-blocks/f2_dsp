// This is the module containing the f2_dsp and the serdes lanes
// Initially written by Marko Kosunen and Paul Rigge, May 2018
//
// Last modification by Marko Kosunen, marko.kosunen@aalto.fi, 10.08.2018 13:50
/////////////////////////////////////////////////////////////////////////////
package f2_dsp_and_serdes
import chisel3._
import chisel3.util._
import chisel3.experimental._
import dsptools._
import dsptools.numbers._
import freechips.rocketchip.util._
import f2_decimator._
import f2_interpolator._
import f2_rx_path._
import hbwif._
import f2_lane_switch._
import f2_cm_serdes_lane._
import f2_rx_dsp._
import f2_tx_dsp._
import f2_tx_path._
import f2_dsp._
import f2_serdes_test._
import clkdiv_n_2_4_8._

class lane_clock_and_reset extends Bundle {
    val clockRef                = Input(Clock())
    val asyncResetIn            = Input(Bool())
}

class laneioanalog extends Bundle {
     val rx_p = Analog(1.W)
     val rx_n = Analog(1.W)
     val tx_p = Analog(1.W)
     val tx_n = Analog(1.W)
}

class f2_dsp_and_serdes_io(
        val rxinputn           : Int=9,
        val txoutputn          : Int=9,
        val thermo             : Int=5,
        val bin                : Int=4,
        val n                  : Int=16,
        val resolution         : Int=32,
        val antennas           : Int=4,
        val gainbits           : Int=10,
        val txweightbits       : Int=10,
        val rxweightbits       : Int=10,
        val users              : Int=4,
        val numserdes          : Int=2,
        val progdelay          : Int=64,
        val finedelay          : Int=32,
        val neighbours         : Int=4,
        val serdestestmemsize  : Int=scala.math.pow(2,13).toInt
    ) extends Bundle {
        val iptr_A                  = Input(Vec(antennas,DspComplex(SInt(rxinputn.W), SInt(rxinputn.W))))
        val Z                       = Output(Vec(antennas,new dac_io(thermo=thermo,bin=bin)))
        val dsp_ctrl_and_clocks= new f2_dsp_ctrl_io(
            rxinputn         = rxinputn,
            txoutputn        = txoutputn,
            thermo           = thermo,
            bin              = bin,
            n                = n,
            resolution       = resolution,
            antennas         = antennas,
            gainbits         = gainbits,
            users            = users,
            numserdes        = numserdes,
            progdelay        = progdelay,
            serdestestmemsize=serdestestmemsize
        )
    
    val lane_clkrst             = Vec(numserdes,new lane_clock_and_reset())
    val lane_refclk_Ndiv        = Input(UInt(8.W))
    val lane_refclk_reset       = Input(Bool())
    val lane_refclk_shift       = Input(Bool())
    val laneanalog              = Vec(numserdes,new laneioanalog())
}

class f2_dsp_and_serdes (
        rxinputn   : Int=9,
        txoutputn  : Int=9,
        thermo     : Int=5,
        bin        : Int=4,
        n          : Int=16,
        resolution : Int=32,
        antennas   : Int=4,
        users      : Int=4,
        gainbits   : Int=10,
        txweightbits: Int=10,
        fifodepth  : Int=16,
        numserdes  : Int=6,
        neighbours : Int=4,
        progdelay  : Int=64,
        finedelay  : Int=32,
        serdestestmemsize : Int=scala.math.pow(2,13).toInt
    ) extends MultiIOModule {
    val io = IO(
        new f2_dsp_and_serdes_io(
            rxinputn         = rxinputn,
            txoutputn        = txoutputn,
            thermo           = thermo,
            bin              = bin,
            n                = n,
            resolution       = resolution,
            antennas         = antennas,
            gainbits         = gainbits,
            users            = users,
            numserdes        = numserdes,
            progdelay        = progdelay,
            serdestestmemsize=serdestestmemsize
        )
    )
    val userzero   = 0.U.asTypeOf(new usersigs(n=n,users=users))
    val udatazero  = 0.U.asTypeOf(userzero.data)
    val uindexzero = 0.U.asTypeOf(userzero.uindex)
    val iofifozero = 0.U.asTypeOf(new iofifosigs(n=n))
    val datazero   = 0.U.asTypeOf(iofifozero.data)
    val rxindexzero= 0.U.asTypeOf(iofifozero.rxindex)
   

    // clock divider
    val clkrefdiv = Module ( new clkdiv_n_2_4_8 ( n=8) ).io
    clkrefdiv.Ndiv     :=io.lane_refclk_Ndiv
    clkrefdiv.reset_clk:=io.lane_refclk_reset
    clkrefdiv.shift    :=io.lane_refclk_shift


    // RX:s
    // Vec is required to do runtime adressing of an array i.e. Seq is not hardware structure
    // Clock of the RX is at the highest frequency
    val dsp = Module ( new  f2_dsp (
        rxinputn          = rxinputn          ,
        txoutputn         = txoutputn         ,
        thermo            = thermo            ,
        bin               = bin               ,
        n                 = n                 ,
        resolution        = resolution        ,
        antennas          = antennas          ,
        users             = users             ,
        gainbits          = gainbits          ,
        txweightbits      = txweightbits      ,
        fifodepth         = fifodepth         ,
        numserdes         = numserdes         ,
        neighbours        = neighbours        ,
        progdelay         = progdelay         ,
        finedelay         = finedelay         ,
        serdestestmemsize = serdestestmemsize
        )
    ).io
    dsp.ctrl_and_clocks<>io.dsp_ctrl_and_clocks
    dsp.iptr_A<>io.iptr_A
    dsp.Z<>io.Z
    

    ////////////////////////////////////////////////////////////////////////
    // Lane definitions start here
    ////////////////////////////////////////////////////////////////////////
    implicit val c=SerDesConfig()
    implicit val b=BertConfig()
    implicit val m=PatternMemConfig()
    val lanes  = Seq.fill(numserdes){ Module (
        new  f2_cm_serdes_lane ( () => new iofifosigs(n=n)))
    }

    private def iomap[T <: Data] = { x:T => IO(chiselTypeOf(x)) }

    private def syncControlIO(
      lane: ControlBundle, io: ControlBundle,
      rxClock: Clock, rxReset: Bool,
      txClock: Clock, txReset: Bool
    ): Unit = {
      // inputs
      lane.inputMap.foreach { case (name, value) =>
        // io -> fifo -> lane
        val in_fifo = Module(new AsyncQueue(value.signal, depth = 1, sync = 3)).io
        in_fifo.enq.bits  := io.inputMap(name).signal
        value.signal      := in_fifo.deq.bits

        in_fifo.enq_clock := clock
        in_fifo.enq_reset := reset
        in_fifo.deq_clock := rxClock
        in_fifo.deq_reset := rxReset

        in_fifo.deq.ready    := true.B
        in_fifo.enq.valid    := true.B // TODO check if needed
        // Can make assertions on enq.ready and deq.valid for simulation
      }
      // outputs
      lane.outputMap.foreach { case (name, value) =>
        // lane -> fifo -> io
        val out_fifo = Module(new AsyncQueue(value.signal, depth = 1, sync = 3)).io
        out_fifo.enq.bits         := value.signal
        io.outputMap(name).signal := out_fifo.deq.bits

        out_fifo.enq_clock        := txClock
        out_fifo.enq_reset        := txReset
        out_fifo.deq_clock        := clock
        out_fifo.deq_reset        := reset

        out_fifo.deq.ready        := true.B
        out_fifo.enq.valid        := true.B // TODO check if needed
        // Can make assertions on enq.ready and deq.valid for simulation
      }
    }

    //Connect lane control IO's
    val lane_ssio         = lanes.map(_.ssio.map(iomap))
    val lane_encoderio    = lanes.map(_.encoderio.map(iomap))
    val lane_decoderio    = lanes.map(_.decoderio.map(iomap))
    val lane_packetizerio = lanes.map(_.packetizerio.map(iomap))
    val lane_debugio      = lanes.map(_.debugio.map(_.map(iomap)))

    (lanes, lane_ssio).zipped.foreach { case (lane, ssio) =>
      syncControlIO(lane.ssio.get,         ssio.get,         lane.io.rxClock, lane.io.rxReset, lane.io.txClock, lane.io.txReset)
    }
    (lanes, lane_encoderio).zipped.foreach { case (lane, encoderio) =>
      (lane.encoderio, encoderio) match {
        case (Some(l), Some(e)) => 
          syncControlIO(l, e, lane.io.rxClock, lane.io.rxReset, lane.io.txClock, lane.io.txReset)
        case (None, None) =>
        case _ => throw new Exception(s"Encoder IO has invalid state: ${lane.encoderio} should be ${encoderio}")
      }
    }
    (lanes, lane_decoderio).zipped.foreach { case (lane, decoderio) =>
      syncControlIO(lane.decoderio.get,    decoderio.get,    lane.io.rxClock, lane.io.rxReset, lane.io.txClock, lane.io.txReset)
    }
    (lanes, lane_packetizerio).zipped.foreach { case (lane, packetizerio) =>
      syncControlIO(lane.packetizerio.get, packetizerio.get, lane.io.rxClock, lane.io.rxReset, lane.io.txClock, lane.io.txReset)
    }
    (lanes, lane_debugio).zipped.foreach { case (lane, debugio) =>
      (lane.debugio, debugio).zipped.foreach { case (l, d) =>
        syncControlIO(l.get, d.get, lane.io.rxClock, lane.io.rxReset, lane.io.txClock, lane.io.txReset)
      }
    }
    // .get is used because the io's are Options, not Seq
    (lanes,io.lane_clkrst).zipped.map(_.io.asyncResetIn<>_.asyncResetIn)
    //(lanes,io.lane_clkrst).zipped.map(_.io.clockRef<>_.clockRef)
    lanes.map(_.io.clockRef<>clkrefdiv.clkpn.asClock)

   //Connect switchbox to SerDes
   (lanes,dsp.lanes_tx).zipped.map(_.io.data.tx<>_)
   (lanes,dsp.lanes_rx).zipped.map(_.io.data.rx<>_)
}
//This gives you verilog
object f2_dsp_and_serdes extends App {
  chisel3.Driver.execute(args, () => new f2_dsp_and_serdes(rxinputn=9, bin=4,thermo=5, n=16, antennas=4, users=4, fifodepth=16, numserdes=2, serdestestmemsize=scala.math.pow(2,13).toInt ))
}


