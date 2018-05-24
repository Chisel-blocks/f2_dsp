// See LICENSE for license details.
//
//Start with a static tb and try to genererate a gnerator for it
package f2_dsp
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

class f2_dsp_io(
        val rxinputn           : Int=9,
        val txoutputn          : Int=9,
        val thermo             : Int=5,
        val bin                : Int=4,
        val n                  : Int=16,
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
    val decimator_clocks        = new f2_decimator_clocks
    val decimator_controls      = Vec(antennas,new f2_decimator_controls(gainbits=10))
    val adc_clocks              = Input(Vec(antennas,Clock()))
    val user_index              = Input(UInt(log2Ceil(users).W)) //W should be log2 of users
    val antenna_index           = Input(UInt(log2Ceil(antennas).W)) //W should be log2 of users
    val reset_index_count       = Input(Bool())
    val reset_adcfifo           = Input(Bool())
    val reset_outfifo           = Input(Bool())
    val reset_infifo            = Input(Bool())
    val rx_output_mode          = Input(UInt(3.W))
    val input_mode              = Input(UInt(3.W))
    val adc_fifo_lut_mode       = Input(UInt(3.W))
    val adc_lut_write_addr      = Input(UInt(rxinputn.W))
    val adc_lut_write_vals      = Input(Vec(antennas,DspComplex(SInt(rxinputn.W), SInt(rxinputn.W))))
    val adc_lut_write_en        = Input(Bool())
    val lane_clkrst             = Vec(numserdes,new lane_clock_and_reset())
    val laneanalog              = Vec(numserdes,new laneioanalog())
    val from_serdes_scan        = Vec(numserdes,Flipped(DecoupledIO(new iofifosigs(n=n))))
    val from_dsp_scan           = Vec(numserdes,Flipped(DecoupledIO(new iofifosigs(n=n))))
    val dsp_to_serdes_address   = Vec(numserdes,Input(UInt(log2Ceil(neighbours+2).W)))
    val serdes_to_dsp_address   = Vec(neighbours+2,Input(UInt(log2Ceil(numserdes).W)))
    val to_serdes_mode          = Vec(numserdes,Input(UInt(2.W))) //Off/On/Scan
    val to_dsp_mode             = Vec(neighbours+2,Input(UInt(2.W))) //Off/on/scan
    val rx_user_delays          = Input(Vec(antennas, Vec(users,UInt(log2Ceil(progdelay).W))))
    val rx_fine_delays          = Input(Vec(antennas, UInt(log2Ceil(finedelay).W)))
    val rx_user_weights         = Input(Vec(antennas,Vec(users,DspComplex(SInt(rxweightbits.W),SInt(rxweightbits.W)))))
    val rx_Ndiv                 = Input(UInt(8.W))
    val rx_reset_clkdiv         = Input(Bool())
    val rx_clkdiv_shift         = Input(Bool())
    val neighbour_delays        = Input(Vec(neighbours, Vec(users,UInt(log2Ceil(progdelay).W))))
    val serdestest_scan         = new serdes_test_scan_ios(proto=new iofifosigs(n=n,users=users),memsize=serdestestmemsize)
    val reset_dacfifo           = Input(Bool())
    val user_spread_mode        = Input(UInt(3.W))
    val user_sum_mode           = Input(Vec(antennas,UInt(3.W)))
    val user_select_index       = Input(Vec(antennas,UInt(log2Ceil(users).W)))
    val dac_clocks              = Input(Vec(antennas,Clock()))
    val dac_data_mode           = Input(Vec(antennas,UInt(3.W)))
    val dac_lut_write_addr      = Input(Vec(antennas,UInt(txoutputn.W)))

    val dac_lut_write_vals      = Input(Vec(antennas,DspComplex(SInt(txoutputn.W), SInt(txoutputn.W))))
    val dac_lut_write_en        = Vec(antennas,Input(Bool()))
    //val optr_neighbours         = Vec(neighbours,DecoupledIO(new iofifosigs(n=n,users=users)))
    val tx_user_delays          = Input(Vec(antennas, Vec(users,UInt(log2Ceil(progdelay).W))))
    val tx_fine_delays          = Input(Vec(antennas,UInt(log2Ceil(finedelay).W)))
    val tx_user_weights         = Input(Vec(antennas,Vec(users,DspComplex(SInt(txweightbits.W), SInt(txweightbits.W)))))
    val tx_Ndiv                 = Input(UInt(8.W))
    val tx_reset_clkdiv         = Input(Bool())
    val tx_clkdiv_shift         = Input(Bool())
    val interpolator_controls   = Vec(antennas,new f2_interpolator_controls(gainbits=10))
}

class f2_dsp (
        rxinputn   : Int=9,
        txoutputn  : Int=9,
        thermo     : Int=5,
        bin        : Int=4,
        n          : Int=16,
        antennas   : Int=4,
        users      : Int=4,
        gainbits   : Int=10,
        txweightbits: Int=10,
        fifodepth  : Int=128,
        numserdes  : Int=6,
        neighbours : Int=4,
        progdelay  : Int=64,
        finedelay  : Int=32,
        serdestestmemsize : Int=scala.math.pow(2,13).toInt
    ) extends MultiIOModule {
    val io = IO(
        new f2_dsp_io(
            rxinputn         = rxinputn,
            txoutputn        = txoutputn,
            thermo           = thermo,
            bin              = bin,
            n                = n,
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
    
    // clock dividers
    val rxclkdiv = withClock(io.adc_clocks(0))(Module ( new clkdiv_n_2_4_8 ( n=8)).io)
    rxclkdiv.Ndiv:=io.rx_Ndiv
    rxclkdiv.reset_clk:=io.rx_reset_clkdiv
    rxclkdiv.shift:=io.rx_clkdiv_shift
    val txclkdiv = Module ( new clkdiv_n_2_4_8 ( n=8)).io 
    txclkdiv.Ndiv:=io.tx_Ndiv
    txclkdiv.reset_clk:=io.tx_reset_clkdiv
    txclkdiv.shift:=io.tx_clkdiv_shift

    // RX:s
    // Vec is required to do runtime adressing of an array i.e. Seq is not hardware structure
    val rxdsp  = withClock(io.adc_clocks(0))(Module ( new  f2_rx_dsp (inputn=rxinputn, n=n, antennas=antennas,
                                            users=users, fifodepth=fifodepth,
                                            progdelay=progdelay,finedelay=finedelay,
                                            neighbours=neighbours)).io)
    rxdsp.decimator_clocks.cic3clockslow:=rxclkdiv.clkpn.asClock 
    rxdsp.decimator_clocks.hb1clock_low :=rxclkdiv.clkp2n.asClock 
    rxdsp.decimator_clocks.hb2clock_low :=rxclkdiv.clkp4n.asClock
    rxdsp.decimator_clocks.hb3clock_low :=rxclkdiv.clkp8n.asClock
    rxdsp.clock_symrate                 :=rxclkdiv.clkp8n.asClock
    rxdsp.clock_symratex4               :=rxclkdiv.clkp2n.asClock
    //Check clocking
    rxdsp.clock_infifo_enq.map(_<>rxclkdiv.clkp8n.asClock)
    rxdsp.clock_outfifo_deq<>rxclkdiv.clkp2n.asClock

    //For TX, the mster clock is the slowest, faster clocks are formed from the system master clock.
    val txdsp  = withClock(txclkdiv.clkp8n.asClock)(Module ( new  f2_tx_dsp (outputn=rxinputn, n=n, antennas=antennas,
                                            users=users, fifodepth=fifodepth,
                                            progdelay=progdelay,finedelay=finedelay,
                                            neighbours=neighbours,weightbits=txweightbits)).io)
    
    txdsp.interpolator_clocks.cic3clockfast   := clock
    txdsp.interpolator_clocks.hb3clock_high   := txclkdiv.clkpn.asClock 
    txdsp.interpolator_clocks.hb2clock_high   := txclkdiv.clkp2n.asClock 
    txdsp.interpolator_clocks.hb1clock_high   := txclkdiv.clkp4n.asClock
    txdsp.clock_symrate                       := txclkdiv.clkp8n.asClock
    txdsp.clock_outfifo_deq                   := txclkdiv.clkp8n.asClock

    //Map io inputs
    //Rx
    rxdsp.iptr_A             :=io.iptr_A
    rxdsp.decimator_controls :=io.decimator_controls
    rxdsp.adc_clocks         :=io.adc_clocks
    rxdsp.user_index         :=io.user_index
    rxdsp.antenna_index      :=io.antenna_index
    rxdsp.reset_index_count  :=io.reset_index_count
    rxdsp.reset_adcfifo      :=io.reset_adcfifo
    rxdsp.reset_outfifo      :=io.reset_outfifo
    rxdsp.reset_infifo       :=io.reset_infifo
    rxdsp.rx_output_mode     :=io.rx_output_mode
    rxdsp.input_mode         :=io.input_mode
    rxdsp.adc_fifo_lut_mode  :=io.adc_fifo_lut_mode
    rxdsp.adc_lut_write_addr :=io.adc_lut_write_addr
    rxdsp.adc_lut_write_vals :=io.adc_lut_write_vals
    rxdsp.adc_lut_write_en   :=io.adc_lut_write_en
    rxdsp.rx_user_delays     :=io.rx_user_delays
    rxdsp.rx_fine_delays     :=io.rx_fine_delays
    rxdsp.rx_user_weights    :=io.rx_user_weights
    rxdsp.neighbour_delays   :=io.neighbour_delays
    //Tx
    txdsp.interpolator_controls <> io.interpolator_controls
    txdsp.dac_clocks         <> io.dac_clocks
    txdsp.reset_dacfifo      <> io.reset_dacfifo
    txdsp.user_spread_mode   <> io.user_spread_mode
    txdsp.user_sum_mode      <> io.user_sum_mode
    txdsp.user_select_index  <> io.user_select_index
    txdsp.dac_data_mode      <> io.dac_data_mode
    txdsp.dac_lut_write_addr <> io.dac_lut_write_addr
    txdsp.dac_lut_write_vals <> io.dac_lut_write_vals
    txdsp.dac_lut_write_en   <> io.dac_lut_write_en
    txdsp.Z                  <> io.Z
    txdsp.tx_user_delays     := io.tx_user_delays
    txdsp.tx_fine_delays     := io.tx_fine_delays
    txdsp.tx_user_weights    := io.tx_user_weights

    val switchbox = Module (
        new f2_lane_switch (
            n=n,
            todspios=neighbours+2,
            fromdspios=neighbours+2,
            serdesios=numserdes
        )
    ).io
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
    (lanes,io.lane_clkrst).zipped.map(_.io.clockRef<>_.clockRef)
    val proto=new iofifosigs(n=n,users=users)
    val serdestest  = Module ( new  f2_serdes_test(proto=proto,n=n,users=users,memsize=serdestestmemsize)).io
   // Map serdestest IOs
   serdestest.scan<>io.serdestest_scan
   //Switchbox controls
   switchbox.from_serdes_scan     <> io.from_serdes_scan
   switchbox.from_dsp_scan        <> io.from_dsp_scan
   switchbox.dsp_to_serdes_address<> io.dsp_to_serdes_address
   switchbox.serdes_to_dsp_address<> io.serdes_to_dsp_address
   switchbox.to_serdes_mode       <> io.to_serdes_mode
   switchbox.to_dsp_mode          <> io.to_dsp_mode

   //Connect RX DSP to switchbox
   rxdsp.ofifo<>switchbox.from_dsp(0)
   (rxdsp.iptr_fifo.take(neighbours),switchbox.to_dsp.slice(1,neighbours+1)).zipped.map(_<>_)

   // Test input for memory, last indexes
   serdestest.to_serdes<>switchbox.from_dsp(neighbours+1)
   serdestest.from_serdes<>switchbox.to_dsp(neighbours+1)


   //Connect TX DSP to switchbox
   txdsp.iptr_A<>switchbox.to_dsp(0)
   (txdsp.optr_neighbours.take(neighbours),switchbox.from_dsp.slice(1,neighbours+1)).zipped.map(_<>_)

   //Connect switchbox to SerDes
   (lanes,switchbox.to_serdes).zipped.map(_.io.data.tx<>_)
   (lanes,switchbox.from_serdes).zipped.map(_.io.data.rx<>_)
}
//This gives you verilog
object f2_dsp extends App {
  chisel3.Driver.execute(args, () => new f2_dsp(rxinputn=9, bin=4,thermo=5, n=16, antennas=4, users=4, fifodepth=128, numserdes=2, serdestestmemsize=scala.math.pow(2,13).toInt ))
}


