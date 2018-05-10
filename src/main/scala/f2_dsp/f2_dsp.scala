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
import f2_rx_path._
import hbwif._
import f2_lane_switch._
import f2_cm_serdes_lane._
import f2_rx_dsp._
import f2_serdes_test._

class lane_clock_and_reset extends Bundle {
    val clockRef                = Input(Clock())
    val asyncResetIn            = Input(Bool())
    val txClock                 = Output(Clock())
    val txReset                 = Output(Bool())
    val rxClock                 = Output(Clock())
    val rxReset                 = Output(Bool())
}

class laneioanalog extends Bundle {
     val rx_p = Analog(1.W)  
     val rx_n = Analog(1.W)  
     val tx_p = Analog(1.W)  
     val tx_n = Analog(1.W)  
}


class f2_dsp_io(
        val rxinputn           : Int=9, 
        val n                  : Int=16, 
        val antennas           : Int=4, 
        val gainbits           : Int=10, 
        val users              : Int=4,
        val numserdes          : Int=2, 
        val todspios           : Int=4,
        val fromdspios         : Int=4,
        val progdelay          : Int=64,
        val finedelay          : Int=32,
        val neighbours         : Int=4,
        val serdestestmemsize  : Int=scala.math.pow(2,13).toInt
    ) extends Bundle {
    val iptr_A                  = Input(Vec(antennas,DspComplex(SInt(rxinputn.W), SInt(rxinputn.W))))
    val decimator_clocks        = new f2_decimator_clocks    
    val decimator_controls      = Vec(antennas,new f2_decimator_controls(gainbits=10))    
    val adc_clocks              = Input(Vec(antennas,Clock()))
    val clock_symrate           = Input(Clock())
    val clock_symratex4         = Input(Clock())
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
    val dsp_to_serdes_address   = Vec(numserdes,Input(UInt(log2Ceil(fromdspios).W))) 
    val serdes_to_dsp_address   = Vec(todspios,Input(UInt(log2Ceil(numserdes).W)))  
    val to_serdes_mode          = Vec(numserdes,Input(UInt(2.W))) //Off/On/Scan
    val to_dsp_mode             = Vec(todspios,Input(UInt(2.W))) //Off/on/scan
    val rx_user_delays          = Input(Vec(antennas, Vec(users,UInt(log2Ceil(progdelay).W))))
    val rx_fine_delays          = Input(Vec(antennas, UInt(log2Ceil(finedelay).W)))
    val neighbour_delays        = Input(Vec(neighbours, Vec(users,UInt(log2Ceil(progdelay).W))))
    val serdestest_scan         = new serdes_test_scan_ios(n=n,users=users,memsize=serdestestmemsize)
}

class f2_dsp (
        rxinputn   : Int=9, 
        n          : Int=16, 
        antennas   : Int=4,
        users      : Int=4,
        gainbits   : Int=10, 
        fifodepth  : Int=128, 
        numserdes  : Int=6,
        todspios   : Int=5,
        fromdspios : Int=2,
        neighbours : Int=4,
        progdelay  : Int=64,
        finedelay  : Int=32,
        serdestestmemsize : Int=scala.math.pow(2,13).toInt
    ) extends MultiIOModule {
    val io = IO( 
        new f2_dsp_io(
            rxinputn         = rxinputn,
            n                = n,
            antennas         = antennas,
            gainbits         = gainbits,
            users            = users,
            numserdes        = numserdes,
            todspios         = todspios,
            fromdspios       = fromdspios,
            progdelay        = progdelay,
            serdestestmemsize=serdestestmemsize
        )
    )
    //val z = new usersigzeros(n=n, users=users)
    val userzero   = 0.U.asTypeOf(new usersigs(n=n,users=users))
    val udatazero  = 0.U.asTypeOf(userzero.data)
    val uindexzero = 0.U.asTypeOf(userzero.uindex)
    val iofifozero = 0.U.asTypeOf(new iofifosigs(n=n))
    val datazero   = 0.U.asTypeOf(iofifozero.data)
    val rxindexzero= 0.U.asTypeOf(iofifozero.rxindex)

    //-The RX:s
    // Vec is required to do runtime adressing of an array i.e. Seq is not hardware structure
    val rxdsp  = Module ( new  f2_rx_dsp (inputn=rxinputn, n=n, antennas=antennas, 
                                            users=users, fifodepth=fifodepth,
                                            progdelay=progdelay,finedelay=finedelay, 
                                            neighbours=neighbours)).io
 
    //Map io inputs
    rxdsp.iptr_A             :=io.iptr_A             
    rxdsp.decimator_clocks   :=io.decimator_clocks   
    rxdsp.decimator_controls :=io.decimator_controls 
    rxdsp.adc_clocks         :=io.adc_clocks         
    rxdsp.clock_symrate      :=io.clock_symrate      
    rxdsp.clock_symratex4    :=io.clock_symratex4    
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
    rxdsp.neighbour_delays   :=io.neighbour_delays 


    val switchbox = Module ( 
        new f2_lane_switch (
            n=n, 
            todspios=todspios, 
            fromdspios=fromdspios,
            serdesios=numserdes 
        ) 
    ).io
    implicit val c=SerDesConfig()
    implicit val b=BertConfig()
    implicit val m=PatternMemConfig()
    val lanes  = Seq.fill(numserdes){ Module ( 
        new  f2_cm_serdes_lane ( () => new iofifosigs(n=n)))
    }

    //Connect lanei control IO's
    private def iomap[T <: Data] = { x:T => IO(chiselTypeOf(x)).asInstanceOf[T] }
    val lane_ssio         = lanes.map(_.ssio.map(iomap))
    val lane_encoderio    =lanes.map(_.encoderio.map(iomap))
    val lane_decoderio    =lanes.map(_.decoderio.map(iomap))
    val lane_packetizerio =lanes.map(_.packetizerio.map(iomap))
    val lane_debugio      =lanes.map(_.debugio.map(_.map(iomap)))
    // .get is used because the io's are Options, not Seq:w
    (lanes,lane_ssio).zipped.map(_.ssio.get<>_.get)
    (lanes,lane_decoderio).zipped.map(_.decoderio.get<>_.get)
    (lanes,lane_packetizerio).zipped.map(_.packetizerio.get<>_.get)
    (lanes,lane_debugio).zipped.map{ case(l,d) => (l.debugio,d).zipped.map(_.get<>_.get)}
    (lanes,io.lane_clkrst).zipped.map(_.io.asyncResetIn<>_.asyncResetIn)
    (lanes,io.lane_clkrst).zipped.map(_.io.clockRef<>_.clockRef)
    (lanes,io.lane_clkrst).zipped.map(_.io.txClock<>_.txClock)
    (lanes,io.lane_clkrst).zipped.map(_.io.txReset<>_.txReset)
    (lanes,io.lane_clkrst).zipped.map(_.io.rxClock<>_.rxClock)
    (lanes,io.lane_clkrst).zipped.map(_.io.rxReset<>_.rxReset)
    
    val serdestest  = Module ( new  f2_serdes_test(n=n,users=users,memsize=serdestestmemsize)).io
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
     (rxdsp.iptr_fifo.take(neighbours),switchbox.to_dsp.take(neighbours)).zipped.map(_<>_)
     
     // Test input for memory
     serdestest.to_serdes<>switchbox.from_dsp(1)
     serdestest.from_serdes<>switchbox.to_dsp(neighbours)

     //Chenck clocking
     rxdsp.clock_infifo_enq.map(_<>io.clock_symrate)
     rxdsp.clock_outfifo_deq<>io.clock_symratex4
      
     //Connect TX DSP to switchbox
     //rxdsp.ofifo<>switchbox.from_dsp(0)
     
     //Connect switchbox to SerDes
     (lanes,switchbox.to_serdes).zipped.map(_.io.data.tx<>_)
     (lanes,switchbox.from_serdes).zipped.map(_.io.data.rx<>_)



   //TODO: Write a mechanism to control the READY signal for rx_dsp output fifo
   // Can ready be a clock?

}
//This gives you verilog
object f2_dsp extends App {
  chisel3.Driver.execute(args, () => new f2_dsp(rxinputn=9, n=16, antennas=4, users=4, fifodepth=128, numserdes=2, serdestestmemsize=scala.math.pow(2,13).toInt ))
}


