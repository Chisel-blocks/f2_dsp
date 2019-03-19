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
import f2_lane_switch._
import f2_cm_serdes_lane._
import f2_rx_dsp._
import f2_tx_dsp._
import f2_tx_path._
import f2_serdes_test._
import clkdiv_n_2_4_8._
import decouple_branch._
import dcpipe._

class f2_dsp_ctrl_io(
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
        val progdelay          : Int=63,
        val finedelay          : Int=31,
        val neighbours         : Int=4,
        val serdestestmemsize  : Int=scala.math.pow(2,13).toInt
    ) extends Bundle {
    val decimator_controls      = Vec(antennas,new f2_decimator_controls(resolution=resolution,gainbits=10))
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
    val adc_lut_reset           = Input(Bool())
    // These are to provide constants from scan as
    // serdes output (from_serdes) and intput (from_dsp)
    val from_serdes_scan        = Vec(numserdes+2,Flipped(DecoupledIO(new iofifosigs(n=n,users=users))))
    val from_dsp_scan           = Vec(numserdes+2,Flipped(DecoupledIO(new iofifosigs(n=n,users=users))))
    //switch address space rx_dsp+tx_to_neighbours+frim_mem_to_serdes
    val dsp_to_serdes_address   = Vec(numserdes+2,Input(UInt(log2Ceil(neighbours+2).W)))
    //switch address space serdeses+dsp_rx_to_memory+memory_to_tx
    val serdes_to_dsp_address   = Vec(neighbours+2,Input(UInt(log2Ceil(numserdes+2).W)))
    val to_serdes_mode          = Vec(numserdes+2,Input(UInt(2.W))) //Off/On/Scan
    val to_dsp_mode             = Vec(neighbours+2,Input(UInt(2.W))) //Off/on/scan
    val rx_user_delays          = Input(Vec(antennas, Vec(users,UInt(log2Ceil(progdelay).W))))
    val rx_fine_delays          = Input(Vec(antennas, UInt(log2Ceil(finedelay).W)))
    val rx_user_weights         = Input(Vec(antennas,Vec(users,DspComplex(SInt(rxweightbits.W),SInt(rxweightbits.W)))))
    val rx_Ndiv                 = Input(UInt(8.W))
    val rx_reset_clkdiv         = Input(Bool())
    val rx_clkdiv_shift         = Input(UInt(2.W))
    val neighbour_delays        = Input(Vec(neighbours, Vec(users,UInt(log2Ceil(progdelay).W))))
    val serdestest_scan         = new serdes_test_scan_ios(proto=new iofifosigs(n=n,users=users),memsize=serdestestmemsize)
    val reset_dacfifo           = Input(Bool())
    val user_spread_mode        = Input(UInt(3.W))
    val user_sum_mode           = Input(Vec(antennas,UInt(3.W)))
    val user_select_index       = Input(Vec(antennas,UInt(log2Ceil(users).W)))
    val dac_clocks              = Input(Vec(antennas,Clock()))
    val dac_data_mode           = Input(Vec(antennas,UInt(3.W)))
    val inv_adc_clk_pol         = Input(Vec(antennas,Bool()))
    val dac_lut_write_addr      = Input(Vec(antennas,UInt(txoutputn.W)))
    val dac_lut_write_vals      = Input(Vec(antennas,DspComplex(SInt(txoutputn.W), SInt(txoutputn.W))))
    val dac_lut_write_en        = Vec(antennas,Input(Bool()))
    val tx_user_delays          = Input(Vec(antennas, Vec(users,UInt(log2Ceil(progdelay).W))))
    val tx_fine_delays          = Input(Vec(antennas,UInt(log2Ceil(finedelay).W)))
    val tx_user_weights         = Input(Vec(antennas,Vec(users,DspComplex(SInt(txweightbits.W), SInt(txweightbits.W)))))
    val tx_Ndiv                 = Input(UInt(8.W))
    val tx_reset_clkdiv         = Input(Bool())
    val tx_clkdiv_shift         = Input(UInt(2.W))
    val interpolator_controls   = Vec(antennas,new f2_interpolator_controls(resolution=resolution,gainbits=10))
}

class f2_dsp_io(
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
        val progdelay          : Int=63,
        val finedelay          : Int=31,
        val neighbours         : Int=4,
        val serdestestmemsize  : Int=scala.math.pow(2,13).toInt
    ) extends Bundle {
         val ctrl_and_clocks= new f2_dsp_ctrl_io(
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
        val MASTER_CLOCK            = Input(Clock())
        val iptr_A                  = Input(Vec(antennas,DspComplex(SInt(rxinputn.W), SInt(rxinputn.W))))
        val Z                       = Output(Vec(antennas,new dac_io(thermo=thermo,bin=bin)))
    // In SerDes, TX is a input for the transmitter, RX is the output of the receiver
    // Thus, lanes_tx is an output, lanes_rx is an input
    val lanes_rx                =Vec(numserdes,Flipped(DecoupledIO(new iofifosigs(n=n,users=users))))
    val lanes_tx                =Vec(numserdes,DecoupledIO(new iofifosigs(n=n,users=users)))
    val lanes_rx_deq_clock      = Input(Clock())  // rx refers to rx of the serdes
                                                   // read samples from lanes_rx to RF transmitter
    val lanes_tx_enq_clock      = Input(Clock())  // Tx refers to serdes tx.
                                                  // reads samples from RF receiveer
    }

class f2_dsp (
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
        progdelay  : Int=63,
        finedelay  : Int=31,
        serdestestmemsize : Int=scala.math.pow(2,13).toInt
    ) extends MultiIOModule {
     val io = IO(
         new f2_dsp_io(
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
     val iofifozero = 0.U.asTypeOf(new iofifosigs(n=n,users=users))
     val datazero   = 0.U.asTypeOf(iofifozero.data)
     val rxindexzero= 0.U.asTypeOf(iofifozero.rxindex)

     // clock dividers
     val rxclkdiv = withClock(io.ctrl_and_clocks.adc_clocks(0))(Module ( new clkdiv_n_2_4_8 ( n=8)).io)
     rxclkdiv.Ndiv:=io.ctrl_and_clocks.rx_Ndiv
     rxclkdiv.reset_clk:=io.ctrl_and_clocks.rx_reset_clkdiv
     rxclkdiv.shift:=io.ctrl_and_clocks.rx_clkdiv_shift

     val txclkdiv = Module ( new clkdiv_n_2_4_8 ( n=8)).io
     txclkdiv.Ndiv:=io.ctrl_and_clocks.tx_Ndiv
     txclkdiv.reset_clk:=io.ctrl_and_clocks.tx_reset_clkdiv
     txclkdiv.shift:=io.ctrl_and_clocks.tx_clkdiv_shift

     // RX:s
     // Vec is required to do runtime adressing of an array i.e. Seq is not hardware structure
     // Clock of the RX is at the highest frequency (Master clock)
     val rxdsp  = Module ( new  f2_rx_dsp (inputn=rxinputn, n=n, antennas=antennas,
                                             users=users, fifodepth=fifodepth,
                                             progdelay=progdelay,finedelay=finedelay,
                                             neighbours=neighbours)).io
     
     //Check clocking
     val proto=new iofifosigs(n=n,users=users)
     val pipestages=4

     //This is always at symbol rate, no neet for faster 
     val rxdsp_inpipe = Seq.fill(neighbours){ 
        withClockAndReset(io.lanes_rx_deq_clock, io.ctrl_and_clocks.reset_infifo){ 
         Module(new dcpipe(proto.cloneType,latency=pipestages)).io
       } 
     }

     // For TX, the master clock is the slowest,
     // faster clocks are formed from the system master clock.
     val txdsp  = withClock(txclkdiv.clkp8n.asClock)(Module (
         new  f2_tx_dsp (outputn=rxinputn, n=n, antennas=antennas,
                                           users=users, fifodepth=fifodepth,
                                           progdelay=progdelay,finedelay=finedelay,
                                           neighbours=neighbours,weightbits=txweightbits)).io
     )
    // Pipeline stages to alleviate place and route
     val txpipe = withClock{io.lanes_rx_deq_clock}(Module ( new dcpipe(proto.cloneType,latency=pipestages)).io)
     val dacproto = new dac_io(thermo=thermo,bin=bin)
     val aint=(0 until antennas ).toList
     val dacpipe = aint.map{index => 
        withClockAndReset(io.ctrl_and_clocks.dac_clocks(index), 
            io.ctrl_and_clocks.reset_dacfifo){ 
                Module(new Pipe(dacproto.cloneType,latency=pipestages)).io
       } 
     }
     dacpipe.map(_.enq.valid:=true.B)


     //Map io inputs
     //Rx
     rxdsp.decimator_clocks.cic3clockslow:=rxclkdiv.clkpn.asClock
     rxdsp.decimator_clocks.hb1clock_low :=rxclkdiv.clkp2n.asClock
     rxdsp.decimator_clocks.hb2clock_low :=rxclkdiv.clkp4n.asClock
     rxdsp.decimator_clocks.hb3clock_low :=rxclkdiv.clkp8n.asClock
     rxdsp.clock_symrate                 :=rxclkdiv.clkp8n.asClock
     rxdsp.clock_symratex4               :=rxclkdiv.clkp2n.asClock
     rxdsp.clock_infifo_enq:=io.lanes_rx_deq_clock   
     rxdsp.clock_outfifo_deq:=io.lanes_tx_enq_clock   //Should be faster than 5xsymrate
                                                      // If we support serialization
     rxdsp.iptr_A             :=io.iptr_A
     rxdsp.decimator_controls :=io.ctrl_and_clocks.decimator_controls
     rxdsp.adc_clocks         :=io.ctrl_and_clocks.adc_clocks
     rxdsp.user_index         :=io.ctrl_and_clocks.user_index
     rxdsp.antenna_index      :=io.ctrl_and_clocks.antenna_index
     rxdsp.reset_index_count  :=io.ctrl_and_clocks.reset_index_count
     rxdsp.reset_adcfifo      :=io.ctrl_and_clocks.reset_adcfifo
     rxdsp.reset_outfifo      :=io.ctrl_and_clocks.reset_outfifo
     rxdsp.reset_infifo       :=io.ctrl_and_clocks.reset_infifo
     rxdsp.rx_output_mode     :=io.ctrl_and_clocks.rx_output_mode
     rxdsp.input_mode         :=io.ctrl_and_clocks.input_mode
     rxdsp.adc_fifo_lut_mode  :=io.ctrl_and_clocks.adc_fifo_lut_mode
     rxdsp.inv_adc_clk_pol    :=io.ctrl_and_clocks.inv_adc_clk_pol
     rxdsp.adc_lut_write_addr :=io.ctrl_and_clocks.adc_lut_write_addr
     rxdsp.adc_lut_write_vals :=io.ctrl_and_clocks.adc_lut_write_vals
     rxdsp.adc_lut_write_en   :=io.ctrl_and_clocks.adc_lut_write_en
     rxdsp.adc_lut_reset      :=io.ctrl_and_clocks.adc_lut_reset
     rxdsp.rx_user_delays     :=io.ctrl_and_clocks.rx_user_delays
     rxdsp.rx_fine_delays     :=io.ctrl_and_clocks.rx_fine_delays
     rxdsp.rx_user_weights    :=io.ctrl_and_clocks.rx_user_weights
     rxdsp.neighbour_delays   :=io.ctrl_and_clocks.neighbour_delays
     rxdsp.clock_outfifo_deq  :=io.lanes_tx_enq_clock
     //
     //Tx
     txdsp.interpolator_clocks.cic3clockfast   := clock
     txdsp.interpolator_clocks.hb3clock_high   := txclkdiv.clkpn.asClock
     txdsp.interpolator_clocks.hb2clock_high   := txclkdiv.clkp2n.asClock
     txdsp.interpolator_clocks.hb1clock_high   := txclkdiv.clkp4n.asClock
     txdsp.interpolator_clocks.hb1clock_low    := txclkdiv.clkp8n.asClock
     txdsp.clock_symrate                       := txclkdiv.clkp8n.asClock
     txdsp.interpolator_controls <> io.ctrl_and_clocks.interpolator_controls
     txdsp.dac_clocks         <> io.ctrl_and_clocks.dac_clocks
     txdsp.reset_dacfifo      <> io.ctrl_and_clocks.reset_dacfifo
     txdsp.reset_infifo       <> io.ctrl_and_clocks.reset_infifo
     txdsp.infifo_enq_clock   <> io.lanes_rx_deq_clock
     txdsp.user_spread_mode   <> io.ctrl_and_clocks.user_spread_mode
     txdsp.user_sum_mode      <> io.ctrl_and_clocks.user_sum_mode
     txdsp.user_select_index  <> io.ctrl_and_clocks.user_select_index
     txdsp.dac_data_mode      <> io.ctrl_and_clocks.dac_data_mode
     txdsp.dac_lut_write_addr <> io.ctrl_and_clocks.dac_lut_write_addr
     txdsp.dac_lut_write_vals <> io.ctrl_and_clocks.dac_lut_write_vals
     txdsp.dac_lut_write_en   <> io.ctrl_and_clocks.dac_lut_write_en
     (dacpipe,txdsp.Z).zipped.map(_.enq.bits:= _)
     (io.Z,dacpipe).zipped.map(_:= _.deq.bits)
     //txdsp.Z                  <> io.Z
     txdsp.tx_user_delays     := io.ctrl_and_clocks.tx_user_delays
     txdsp.tx_fine_delays     := io.ctrl_and_clocks.tx_fine_delays
     txdsp.tx_user_weights    := io.ctrl_and_clocks.tx_user_weights

     val switchbox = Module (
         new f2_lane_switch (
             proto=proto.cloneType,
             todspios=neighbours+2,
             fromdspios=neighbours+2,
             serdesios=numserdes+2
         )
     ).io

     //SerDes Test Memory and state machine
     val serdestest  = withClock(io.lanes_tx_enq_clock)(
         Module ( new  f2_serdes_test(
             proto=proto,
             n=n,
             users=users,
             fifodepth=fifodepth,
             memsize=serdestestmemsize)).io
     )

     // Map serdestest IOs
     serdestest.scan<>io.ctrl_and_clocks.serdestest_scan
     //Switchbox controls
     switchbox.from_serdes_scan     <> io.ctrl_and_clocks.from_serdes_scan
     switchbox.from_dsp_scan        <> io.ctrl_and_clocks.from_dsp_scan
     switchbox.dsp_to_serdes_address<> io.ctrl_and_clocks.dsp_to_serdes_address
     switchbox.serdes_to_dsp_address<> io.ctrl_and_clocks.serdes_to_dsp_address
     switchbox.to_serdes_mode       <> io.ctrl_and_clocks.to_serdes_mode
     switchbox.to_dsp_mode          <> io.ctrl_and_clocks.to_dsp_mode

     //Connect RX DSP to switchbox
     // TODO: if not ready, signals are zeroed. Should be (asynchronously) held
     val rx_ofifo_branch= Module ( new decouple_branch(proto=proto,n=2)).io
     rxdsp.ofifo<>rx_ofifo_branch.Ai
     rx_ofifo_branch.Bo(0)<>switchbox.from_dsp(0)
     rx_ofifo_branch.Bo(1)<>switchbox.from_serdes(numserdes)
     
     // Rxdsp neighbour inputs
     (rxdsp_inpipe,switchbox.to_dsp.slice(1,neighbours+1)).zipped.map(_.enq<>_)
     (rxdsp_inpipe,rxdsp.iptr_fifo.take(neighbours)).zipped.map(_.deq<>_)
     //(rxdsp.iptr_fifo.take(neighbours),switchbox.to_dsp.slice(1,neighbours+1)).zipped.map(_<>_)

     // Test input for memory, last indexes
     val serdestest_branch= Module ( new decouple_branch(proto=proto,n=2)).io
     serdestest_branch.Ai<>serdestest.to_serdes
     serdestest_branch.Bo(0)<>switchbox.from_dsp(neighbours+1)
     // This enables writing to TX from memory
     serdestest_branch.Bo(1)<>switchbox.from_serdes(numserdes+1)

     serdestest.from_serdes<>switchbox.to_dsp(neighbours+1)


     //Connect TX DSP to switchbox
     txpipe.enq<>switchbox.to_dsp(0) 
     txdsp.iptr_A<>txpipe.deq     
     //End index of slice is exclusive
     (txdsp.optr_neighbours.take(neighbours),switchbox.from_dsp.slice(1,neighbours+1)).zipped.map(_<>_)

     // Add buffer pipes for serdes IO's
     val serdestxpipe = Seq.fill(numserdes){ withClock(io.lanes_tx_enq_clock){ Module(new dcpipe(proto.cloneType,latency=pipestages)).io} }

     //Connect switchbox to SerDes IO
     (switchbox.to_serdes.take(numserdes),serdestxpipe).zipped.map(_<>_.enq)
     (io.lanes_tx,serdestxpipe).zipped.map(_<>_.deq)
     (io.lanes_rx,switchbox.from_serdes.take(numserdes)).zipped.map(_<>_)
     //Outputs are ready, althoug floating
     switchbox.to_serdes.slice(numserdes,numserdes+2).map(_.ready:=1.U)
     //switchbox.to_serdes(numserdes).ready:=1.U
     //switchbox.to_serdes(numserdes+1).ready:=1.U

}
//This gives you verilog
object f2_dsp extends App {
  chisel3.Driver.execute(args, () => new f2_dsp(rxinputn=9, bin=4,thermo=5, n=16, antennas=4, users=16, progdelay=63, finedelay=31, fifodepth=16, numserdes=2, serdestestmemsize=scala.math.pow(2,13).toInt ))
}

