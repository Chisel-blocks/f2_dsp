// This uses clkdiv_n_2_4_8 verilog. You need to compile it separately

package f2_dsp

import chisel3._
import java.io.{File, FileWriter, BufferedWriter}
import com.gilt.handlebars.scala.binding.dynamic._
import com.gilt.handlebars.scala.Handlebars

//Testbench.
object tb_f2_dsp {
    // This is insane
    // This must be done by a method processing direction-name-width tuples
    def main(args: Array[String]): Unit = {
         val name= this.getClass.getSimpleName.split("\\$").last
         val tb = new BufferedWriter(new FileWriter("./verilog/"+name+".v"))
         object tbvars {
             val oname=name
             val dutmod = "f2_dsp" 
             val rxantennas=4
             val txantennas=4
             val rxindexbits=2
             val nserdes=2
             val users=16
             val uindexbits=4
             val neighbours=4
             val testmemsize=13
             object tx {
                 //val n = 16
                 //val inputn = 9
                 //val gainbits= 10
                 val inbits=16
                 val scalebits=10
                 val derivshiftbits=5
                 val interpmodebits=3
                 val spreadmodebits=3
                 val summodebits=3
                 val dacdatamodebits=3
                 val outbits=9
                 val txuserdelaybits=6
                 val txweightbits=10
                 val txfinedelaybits=5
                 val thermo=5
                 val bin=4
             }
             object rx {
                 val n = 16
                 val inbits=9
                 val gainbits= 10
                 val integshiftbits=5
                 val decimator_modebits= 3
                 val rx_output_modebits= 3
                 val input_modebits= 3
                 val adc_fifo_lut_modebits= 3
                 val adc_lut_width= 9
                 val delay_width      = 6
                 val fine_delay_width = 5
                 val weight_width     = 10
             }

             val iofilesparamseq=Seq(
                           ("in","g_io_iptr_A","\"./io_iptr_A.txt\""), 
                           ("out","g_io_Z","\"./io_Z.txt\""),
                           ("in","g_io_lanes_rx","\"./io_lanes_rx.txt\""), 
                           ("out","g_io_lanes_tx","\"./io_lanes_tx.txt\"")
             )

             val paramseq=Seq(
                           ("g_Rs_high","16*20.0e6"),
                           ("g_Rs_low","20.0e6"),
                           ("g_tx_shift","0"),
                           ("g_tx_scale0","8"),
                           ("g_tx_scale1","2"),
                           ("g_tx_scale2","2"),
                           ("g_tx_scale3","512"),
                           ("g_tx_cic3shift","4"),
                           ("g_tx_user_spread_mode","0"),
                           ("g_tx_user_sum_mode","0"),
                           ("g_tx_user_select_index","0"),
                           ("g_tx_interpolator_mode","4"),
                           ("g_tx_dac_data_mode","6"),
                           //Rx parameters
                           ("g_rx_shift","0"),
                           ("g_rx_scale0","1"),
                           ("g_rx_scale1","16"),
                           ("g_rx_scale2","2"),
                           ("g_rx_scale3","1"),
                           ("g_rx_cic3shift","4"),
                           ("g_rx_user_index","0"),
                           ("g_rx_antenna_index","0"),
                           ("g_rx_output_mode","0"),
                           ("g_rx_input_mode","0"),
                           ("g_rx_mode","4"),
                           ("g_rx_inv_adc_clk_pol","1"),
                           ("g_rx_adc_fifo_lut_mode","2"),
                           ("g_lane_refclk_Ndiv","2"),
                           ("g_lane_refclk_shift","0")
                           )

            //(type,name,upperlimit,lowerlimit, assign,init)    
            //("None","None","None","None","None","None")
            var ioseq=Seq( 
                          ("reg","asyncResetIn_clockRef","None","None","None",1),
                          ("wire","lane_clockRef","None","None","None","None"),
                          ("reg","lane_clkrst_asyncResetIn","None","None","None",1),
                          ("reg","lane_refclk_Ndiv",7,0,"None","g_lane_refclk_Ndiv"),
                          ("reg","lane_refclk_reset","None","None","None",1),
                          ("reg","lane_refclk_shift",1,0,"None","g_lane_refclk_shift"),
                          ("wire","clkp2n","None","None","None","None"), //Not used
                          ("wire","clkp4n","None","None","None","None"), //Not used 
                          ("wire","clkp8n","None","None","None","None"), //Not used
                          ("in","io_ctrl_and_clocks_tx_Ndiv",7,0,"None","tx_c_ratio"),
                          ("in","io_ctrl_and_clocks_tx_reset_clkdiv","None","None","None",1),
                          ("in","io_ctrl_and_clocks_tx_clkdiv_shift",1,0,"None","g_tx_shift"),
                          ("in","io_ctrl_and_clocks_rx_Ndiv",7,0,"None","rx_c_ratio"),
                          ("in","io_ctrl_and_clocks_rx_reset_clkdiv","None","None","None",1),
                          ("in","io_ctrl_and_clocks_rx_clkdiv_shift",1,0,"None","g_rx_shift"),
                          ("reg","reset_loop","None","None","None",1),
                          ("reg","reset_clock_div","None","None","None",1),
                          ("reset","reset","None","None","None",1),
                          ("in","clock","None","None","None","None"),
                          ("in","io_ctrl_and_clocks_reset_dacfifo","None","None","None","'b1"),
                          ("in","io_ctrl_and_clocks_user_spread_mode",tx.spreadmodebits-1,0,"None","g_tx_user_spread_mode")
                          )

                          // Use index loops to reduce typos 
                          // Tx interpolator controls
                          for( i <-0 until txantennas ) { 
                              ioseq++=Seq(
                                  ("in","io_iptr_A_%s_real".format(i).mkString,rx.inbits-1,0,"None","'b0"),
                                  ("in","io_iptr_A_%s_imag".format(i).mkString,rx.inbits-1,0,"None","'b0"),
                                  ("in","io_ctrl_and_clocks_interpolator_controls_%s_cic3derivscale".format(i).mkString,tx.scalebits-1,0,"None","g_tx_scale3"),
                                  ("in","io_ctrl_and_clocks_interpolator_controls_%s_cic3derivshift".format(i).mkString,tx.derivshiftbits-1,0,"None","g_tx_cic3shift"),
                                  ("in","io_ctrl_and_clocks_interpolator_controls_%s_hb1scale".format(i).mkString,tx.scalebits-1,0,"None","g_tx_scale0"),
                                  ("in","io_ctrl_and_clocks_interpolator_controls_%s_hb2scale".format(i).mkString,tx.scalebits-1,0,"None","g_tx_scale1"),
                                  ("in","io_ctrl_and_clocks_interpolator_controls_%s_hb3scale".format(i).mkString,tx.scalebits-1,0,"None","g_tx_scale2"),
                                  ("in","io_ctrl_and_clocks_interpolator_controls_%s_mode".format(i).mkString,tx.interpmodebits-1,0,"None","g_tx_interpolator_mode"),
                                  ("in","io_ctrl_and_clocks_interpolator_controls_%s_reset_loop".format(i).mkString,"None","None","reset_loop","None"),
                                  ("dclk","io_ctrl_and_clocks_dac_clocks_%s".format(i).mkString,"None","None","clock","None"),
                                  ("in","io_ctrl_and_clocks_user_sum_mode_%s".format(i).mkString,tx.summodebits-1,0,"None","g_tx_user_sum_mode"),
                                  ("in","io_ctrl_and_clocks_user_select_index_%s".format(i).mkString,uindexbits-1,0,"None","g_tx_user_select_index"),
                                  ("in","io_ctrl_and_clocks_dac_data_mode_%s".format(i).mkString,tx.dacdatamodebits-1,0,"None","g_tx_dac_data_mode"),
                                  ("in","io_ctrl_and_clocks_dac_lut_write_addr_%s".format(i).mkString,tx.outbits-1,0,"None",0),
                                  ("in","io_ctrl_and_clocks_dac_lut_write_vals_%s_real".format(i).mkString,tx.outbits-1,0,"None",0),
                                  ("in","io_ctrl_and_clocks_dac_lut_write_vals_%s_imag".format(i).mkString,tx.outbits-1,0,"None",0),
                                  ("in","io_ctrl_and_clocks_dac_lut_write_en_%s".format(i).mkString,"None","None","None",0)
                              );
                          }
                          // In SerDes, TX is a input for the transmitter, RX is the output of the receiver
                          // Thus, lanes_tx is an output, lanes_rx is an input
                          ioseq++=Seq(
                              ("out","io_lanes_rx_deq_clock","None","None","None","None"),
                              ("in","io_lanes_tx_enq_clock","None","None","lane_clockRef","None")
                          )

                          //Serdes tx lanes
                          for( i <- 0 until nserdes ){
                              ioseq++=Seq(
                                  ("in","io_lanes_tx_%s_ready".format(i).mkString,"None","None","None","'b1"),
                                  ("out","io_lanes_tx_%s_valid".format(i).mkString,"None","None","None","None"),
                                  ("out","io_lanes_tx_%s_bits_rxindex".format(i).mkString,rxindexbits-1,0,"None","None")
                                )
                              for( k <- 0 until users ){
                                  ioseq++=Seq(
                                      ("outs","io_lanes_tx_%s_bits_data_%s_udata_real".format(i,k).mkString,tx.inbits-1,0,"None","None"),
                                      ("outs","io_lanes_tx_%s_bits_data_%s_udata_imag".format(i,k).mkString,tx.inbits-1,0,"None","None"),
                                      ("out","io_lanes_tx_%s_bits_data_%s_uindex".format(i,k).mkString,uindexbits-1,0,"None","None")
                                 )
                              }
                          }

                          //Serdes rx lanes
                          for( i <- 0 until nserdes ){
                              ioseq++=Seq(
                                  ("out","io_lanes_rx_%s_ready".format(i).mkString,"None","None","None","None"),
                                  ("in","io_lanes_rx_%s_valid".format(i).mkString,"None","None","None","'b1"),
                                  ("in","io_lanes_rx_%s_bits_rxindex".format(i).mkString,rxindexbits-1,0,"None","'b0")
                              )
                              for( k <- 0 until users ){
                                  ioseq++=Seq(
                                      ("in","io_lanes_rx_%s_bits_data_%s_udata_real".format(i,k).mkString,tx.inbits-1,0,"None","'b0"),
                                      ("in","io_lanes_rx_%s_bits_data_%s_udata_imag".format(i,k).mkString,tx.inbits-1,0,"None","'b0"),
                                      ("in","io_lanes_rx_%s_bits_data_%s_uindex".format(i,k).mkString,uindexbits-1,0,"None","'b0")
                                  )
                              }
                          }

                          // Tx delay control
                          for( i <- 0 until txantennas ){
                              ioseq++=Seq(
                                  ("in","io_ctrl_and_clocks_tx_fine_delays_%s".format(i).mkString,tx.txfinedelaybits-1,0,"None",0)
                              )

                              for( k <- 0 until users ){
                                  ioseq++=Seq(
                                      ("in","io_ctrl_and_clocks_tx_user_delays_%s_%s".format(i,k).mkString,tx.txuserdelaybits-1,0,"None",0),
                                      ("in","io_ctrl_and_clocks_tx_user_weights_%s_%s_real".format(i,k).mkString,tx.txweightbits-1,0,"None",1),
                                      ("in","io_ctrl_and_clocks_tx_user_weights_%s_%s_imag".format(i,k).mkString,tx.txweightbits-1,0,"None",1)
                                  )
                              }
                          }

                          // TX outputs
                          for( i <- 0 until txantennas ){
                              ioseq++=Seq(
                                  ("out","io_Z_%s_real_b".format(i).mkString,tx.bin-1,0,"None","None"),
                                  ("out","io_Z_%s_real_t".format(i).mkString,scala.math.pow(2,tx.thermo).toInt-2,0,"None","None"),
                                  ("out","io_Z_%s_imag_b".format(i).mkString,tx.bin-1,0,"None","None"),
                                  ("out","io_Z_%s_imag_t".format(i).mkString,scala.math.pow(2,tx.thermo).toInt-2,0,"None","None")
                              )
                          }
                          //RX starts here
                          for( i <-0 until rxantennas ) { 
                              ioseq++=Seq(
                                  ("in","io_ctrl_and_clocks_decimator_controls_%s_cic3integscale".format(i).mkString,rx.gainbits-1,0,"None","g_rx_scale0"),
                                  ("in","io_ctrl_and_clocks_decimator_controls_%s_cic3integshift".format(i).mkString,rx.integshiftbits-1,0,"None","g_rx_cic3shift"),
                                  ("in","io_ctrl_and_clocks_decimator_controls_%s_reset_loop".format(i).mkString,0,0,"reset_loop","None"),
                                  ("in","io_ctrl_and_clocks_decimator_controls_%s_hb1scale".format(i).mkString,rx.gainbits-1,0,"None","g_rx_scale1"),
                                  ("in","io_ctrl_and_clocks_decimator_controls_%s_hb2scale".format(i).mkString,rx.gainbits-1,0,"None","g_rx_scale2"),
                                  ("in","io_ctrl_and_clocks_decimator_controls_%s_hb3scale".format(i).mkString,rx.gainbits-1,0,"None","g_rx_scale3"),
                                  ("in","io_ctrl_and_clocks_decimator_controls_%s_mode".format(i).mkString,rx.decimator_modebits-1,0,"None","g_rx_mode"),
                                  ("dclk","io_ctrl_and_clocks_adc_clocks_%s".format(i).mkString,"None","None","clock","None"),
                                  ("in","io_ctrl_and_clocks_inv_adc_clk_pol_%s".format(i).mkString,"None","None","None","g_rx_inv_adc_clk_pol"),
                                  ("in","io_ctrl_and_clocks_adc_lut_write_vals_%s_real".format(i).mkString,rx.adc_lut_width-1,0,"None",0),
                                  ("in","io_ctrl_and_clocks_adc_lut_write_vals_%s_imag".format(i).mkString,rx.adc_lut_width-1,0,"None",0)
                              )
                          }
                          // RX controls
                          ioseq++=Seq(
                              ("in","io_ctrl_and_clocks_user_index",uindexbits-1,0,"None","g_rx_user_index"),
                              ("in","io_ctrl_and_clocks_antenna_index",rxindexbits-1,0,"None","g_rx_antenna_index"),
                              ("in","io_ctrl_and_clocks_reset_index_count","None","None","None",1),
                              ("in","io_ctrl_and_clocks_reset_outfifo","None","None","None",1),
                              ("in","io_ctrl_and_clocks_reset_adcfifo","None","None","None",1),
                              ("in","io_ctrl_and_clocks_reset_infifo","None","None","None",1),
                              ("in","io_ctrl_and_clocks_rx_output_mode",rx.rx_output_modebits-1,0,"None","g_rx_output_mode"),
                              ("in","io_ctrl_and_clocks_input_mode",rx.input_modebits-1,0,"None","g_rx_input_mode"),
                              ("in","io_ctrl_and_clocks_adc_fifo_lut_mode",rx.adc_fifo_lut_modebits-1,0,"None","g_rx_adc_fifo_lut_mode"),
                              ("in","io_ctrl_and_clocks_adc_lut_write_addr",rx.adc_lut_width-1,0,"None",0),
                              ("in","io_ctrl_and_clocks_adc_lut_write_en","None","None","None",0),
                              ("in","io_ctrl_and_clocks_adc_lut_reset","None","None","None",1)
                          )

                          // More RX controls
                          for( i <-0 until rxantennas ) { 
                              // Fine_delays
                              ioseq++=Seq(
                                  ("in","io_ctrl_and_clocks_rx_fine_delays_%s".format(i).mkString,rx.fine_delay_width-1,0,"None","'b0")
                              )

                              // User delays and weights
                              for( k <-0 until users ) { 
                                  ioseq++=Seq(
                                      ("in","io_ctrl_and_clocks_rx_user_delays_%s_%s".format(i,k).mkString,rx.delay_width-1,0,"None","'b0"),
                                      ("in","io_ctrl_and_clocks_rx_user_weights_%s_%s_real".format(i,k).mkString,rx.weight_width-1,0,"None","'b1"),
                                      ("in","io_ctrl_and_clocks_rx_user_weights_%s_%s_imag".format(i,k).mkString,rx.weight_width-1,0,"None","'b1")
                                  )
                              }
                          }


                          // Neighbour_delays
                          for( i <-0 until neighbours ) { 
                              // User delays and weights
                              for( k <-0 until users ) { 
                                  ioseq++=Seq(
                                      ("in","io_ctrl_and_clocks_neighbour_delays_%s_%s".format(i,k).mkString,rx.delay_width-1,0,"None","'b0")
                                  )
                              }
                          }

                          //Serdes test stuff
                          for( i <-0 until nserdes+2 ) { 
                              ioseq++=Seq(
                                  ("out","io_ctrl_and_clocks_from_serdes_scan_%s_ready".format(i).mkString,"None","None","None","None"),
                                  ("in","io_ctrl_and_clocks_from_serdes_scan_%s_valid".format(i).mkString,"None","None","None","'b1"),
                                  ("in","io_ctrl_and_clocks_from_serdes_scan_%s_bits_rxindex".format(i).mkString,rxindexbits-1,0,"None","'b0"),
                                  ("out","io_ctrl_and_clocks_from_dsp_scan_%s_ready".format(i).mkString,"None","None","None","None"),
                                  ("in","io_ctrl_and_clocks_from_dsp_scan_%s_valid".format(i).mkString,"None","None","None","'b1"),
                                  ("in","io_ctrl_and_clocks_from_dsp_scan_%s_bits_rxindex".format(i).mkString,rxindexbits-1,0,"None","'b0"),
                                  ("in","io_ctrl_and_clocks_dsp_to_serdes_address_%s".format(i).mkString,2,0,"None","'b0"),
                                  ("in","io_ctrl_and_clocks_to_serdes_mode_%s".format(i).mkString,1,0,"None","'b1")
                              )

                              for( k <-0 until users ) { 
                                  ioseq++=Seq(
                                      ("in","io_ctrl_and_clocks_from_serdes_scan_%s_bits_data_%s_udata_real".format(i,k).mkString,tx.inbits-1,0,"None","'b0"),
                                      ("in","io_ctrl_and_clocks_from_serdes_scan_%s_bits_data_%s_udata_imag".format(i,k).mkString,tx.inbits-1,0,"None","'b0"),
                                      ("in","io_ctrl_and_clocks_from_serdes_scan_%s_bits_data_%s_uindex".format(i,k).mkString,uindexbits-1,0,"None","'b0"),
                                      ("in","io_ctrl_and_clocks_from_dsp_scan_%s_bits_data_%s_udata_real".format(i,k).mkString,rx.n-1,0,"None","'b0"),
                                      ("in","io_ctrl_and_clocks_from_dsp_scan_%s_bits_data_%s_udata_imag".format(i,k).mkString,rx.n-1,0,"None","'b0"),
                                      ("in","io_ctrl_and_clocks_from_dsp_scan_%s_bits_data_%s_uindex".format(i,k).mkString,uindexbits-1,0,"None","'b0")
                                  )
                              }
                          }

                          for( i <-0 until users ) { 
                              ioseq++=Seq(
                                  ("in","io_ctrl_and_clocks_serdestest_scan_write_value_data_%s_udata_real".format(i).mkString,tx.inbits-1,0,"None","'b0"),
                                  ("in","io_ctrl_and_clocks_serdestest_scan_write_value_data_%s_udata_imag".format(i).mkString,tx.inbits-1,0,"None","'b0"),
                                  ("in","io_ctrl_and_clocks_serdestest_scan_write_value_data_%s_uindex".format(i).mkString,uindexbits-1,0,"None","'b0"),
                                  ("outs","io_ctrl_and_clocks_serdestest_scan_read_value_data_%s_udata_real".format(i).mkString,rx.n-1,0,"None","'b0"),
                                  ("outs","io_ctrl_and_clocks_serdestest_scan_read_value_data_%s_udata_imag".format(i).mkString,rx.n-1,0,"None","'b0"),
                                  ("outs","io_ctrl_and_clocks_serdestest_scan_read_value_data_%s_uindex".format(i).mkString,uindexbits-1,0,"None","'b0")
                              )
                          }

                          for( i <-0 until nserdes+neighbours ) { 
                              ioseq++=Seq(
                                      ("in","io_ctrl_and_clocks_serdes_to_dsp_address_%s".format(i).mkString,1,0,"None","'b0"),
                                      ("in","io_ctrl_and_clocks_to_dsp_mode_%s".format(i).mkString,1,0,"None","'b1")
                                  )
                         }

                          ioseq++=Seq(
                              ("in","io_ctrl_and_clocks_serdestest_scan_write_mode",1,0,"None","'b0"),
                              ("in","io_ctrl_and_clocks_serdestest_scan_write_address",testmemsize-1,0,"None","'b0"),
                              ("in","io_ctrl_and_clocks_serdestest_scan_write_value_rxindex",rxindexbits-1,0,"None","'b0"),
                              ("in","io_ctrl_and_clocks_serdestest_scan_write_en","None","None","None","'b0"),
                              ("in","io_ctrl_and_clocks_serdestest_scan_read_mode",1,0,"None","'b0"),
                              ("in","io_ctrl_and_clocks_serdestest_scan_read_address",testmemsize-1,0,"None","'b0"),
                              ("out","io_ctrl_and_clocks_serdestest_scan_read_value_rxindex",rxindexbits-1,0,"None","None"),
                              ("in","io_ctrl_and_clocks_serdestest_scan_read_en","None","None","None","'b0")
                          )
        }
        val header="//This is a tesbench generated with scala generator\n"
        var extpars="""//Things you want to control from the simulator cmdline must be parameters %nmodule %s #(""".format(tbvars.oname)+
                      tbvars.iofilesparamseq.map{ 
                           case (dir,par,value) => "parameter %s = %s,\n            ".format(par,value)
                       }.mkString+
                       tbvars.paramseq.map{ 
                           case (par,value) => "parameter %s = %s,\n            ".format(par,value)
                       }.mkString
        extpars=extpars.patch(extpars.lastIndexOf(','),"",1)+");"

        val iofiledef="""//Variables for the io_files%n""".format()+
                  tbvars.iofilesparamseq.map{
                      case (dir,param,file) => "integer %s;\n".format(param.replace("g_","f_"))
                      case _ => ""
                  }.mkString+
                  tbvars.iofilesparamseq.map{
                      case (dir,param,file) => "integer %s;\n".format(param.replace("g_","status_"))
                      case _ => ""
                  }.mkString+
                  tbvars.iofilesparamseq.map{
                      case ("in",param,file) => "initial %s=$fopen(%s,\"r\");\n".format(param.replace("g_","f_"),param)
                      case ("out",param,file) => "initial %s=$fopen(%s,\"w\");\n".format(param.replace("g_","f_"),param)
                      case _ => ""
                      }.mkString

        val iofileclose="""    //Close the io_files%n""".format()+
                  tbvars.iofilesparamseq.map{
                      case (dir,param,file) => "    $fclose(%s);\n".format(param.replace("g_","f_"),param)
                      case _ => ""
                      }.mkString

        var dutdef="""//DUT definition%n    %s DUT (""".format(tbvars.dutmod)+
                     tbvars.ioseq.map{ 
                         case ("reg",name,ul,dl,assingn,init)  => ""
                         case ("wire",name,ul,dl,assingn,init)  => ""
                         case ("reset"|"clock",name,ul,dl,assign,init)  => ".%s(%s),\n    ".format(name,name)
                         case (dir,name,ul,dl,assign,init) => ".%s(%s),\n    ".format(name,name)
                         case _ => ""
                     }.mkString
        dutdef=dutdef.patch(dutdef.lastIndexOf(','),"",1)+");"

        val regdef="""//Registers for inputs %n""".format() +
                     tbvars.ioseq.map{ 
                         case ("clock",name,ul,dl,assign,init)  => "reg %s;\n".format(name)
                         case ("reset",name,ul,dl,assign,init) => "reg %s;\n".format(name)
                         case ("in"|"reg",name,"None","None",assign,init) => "reg %s;\n".format(name)
                         case ("in"|"reg",name,ul,dl,assign,init) => "reg [%s:%s] %s;\n".format(ul,dl,name)
                         case ("ins"|"regs",name,ul,dl,assign,init) => "reg signed [%s:%s] %s;\n".format(ul,dl,name)
                         case _ => ""
                     }.mkString

        val wiredef="""//Wires for outputs %n""".format() +
                     tbvars.ioseq.map{ 
                         case ("dclk"|"out"|"wire",name,"None","None",assign,init) => "wire %s;\n".format(name)
                         case ("out"|"wire",name,ul,dl,assign,init) => "wire [%s:%s] %s;\n".format(ul,dl,name)
                         case ("outs"|"wires",name,ul,dl,assign,init) => "wire signed [%s:%s] %s;\n".format(ul,dl,name)
                         case _ => ""
                     }.mkString

        val assdef="""//Assignments %n""".format()+
                     tbvars.ioseq.map{ 
                         case ("dclk"|"out"|"in",name,ul,dl,"None",init) => ""
                         case ("dclk"|"out"|"in",name,ul,dl,"clock",init) => "assign %s=clock;\n".format(name)
                         case ("dclk"|"out"|"in",name,ul,dl,"reset",init) => "assign %s=reset;\n".format(name)
                         case ("dclk"|"out"|"in",name,ul,dl,assign,init) => "assign %s=%s;\n".format(name,assign)
                         case _ => ""
                     }.mkString

        val initialdef="""%n%n//Initial values %ninitial #0 begin%n""".format()+
                     tbvars.ioseq.map{ 
                         case ( dir,name,ul,dl,assign,"None") => ""
                         case ("reset",name,ul,dl,assign,init) => "    %s=%s;\n".format(name,init)
                         case ("reset" | "in" | "wire" | "reg" |"wires" | "regs" ,name,ul,dl,assign,init) => "    %s=%s;\n".format(name,init)
                         case _ => ""
                     }.mkString
                     
        val pars="""    |
                        |//timescale 1ps this should probably be a global model parameter 
                        |parameter integer c_Ts=1/(g_Rs_high*1e-12);
                        |parameter tx_c_ratio=g_Rs_high/(8*g_Rs_low);
                        |parameter rx_c_ratio=g_Rs_high/(8*g_Rs_low);
                        |parameter RESET_TIME = 128*c_Ts; // initially 16
                        |
                        |""".stripMargin('|')

        //Parametrize the Tx io signal writing
        var txiosigs=Seq[(String,String,String,String)]() 
            for( i <-0 until tbvars.txantennas ) { 
                 txiosigs++=Seq(("io_Z_%s_real_t".format(i),"io_Z_%s_real_b".format(i), "io_Z_%s_imag_t".format(i), "io_Z_%s_imag_b".format(i)))
             }

        var txiotest=txiosigs.map{ 
                            case (realt,realb,imagt,imagb) => "        ~$isunknown(%s) && ~$isunknown(%s) &&  ~$isunknown(%s) &&  ~$isunknown(%s) &&\n".format(realt,realb,imagt,imagb)
                            }.mkString
        txiotest=txiotest.patch(txiotest.lastIndexOfSlice("&&"),"",2)

        var txiofields =txiosigs.map{ 
                            case name => "%b\\t%d\\t%b\\t%d\\t"
                            }.mkString
        txiofields=txiofields.patch(txiofields.lastIndexOfSlice("\\t"),"",2)

        var txwritenames =txiosigs.map{ 
                            case (realt,realb,imagt,imagb ) => "        %s, %s, %s, %s,\n".format(realt,realb,imagt,imagb)
                            }.mkString
        txwritenames=txwritenames.patch(txwritenames.lastIndexOf(','),");",1)
         
         val txiowrite="""   |//Tx_io
                        |always @(posedge io_ctrl_and_clocks_dac_clocks_0 ) begin 
                        |    //Print only valid values 
                        |    if ((initdone==1) && txdone==0 &&
                        |""".stripMargin('|')+
                        txiotest+
                        """|) begin
                        |        $fwrite(f_io_Z, """".stripMargin('|')+txiofields+"""\n",
                        |""".stripMargin('|')+ txwritenames +
                        """|    end
                        |    //else begin
                        |         //$display( $time, "Dropping invalid output values at io_Z ");
                        |    //end 
                        |end
                        |""".stripMargin('|')

        //Parametrize the Rx io signal writing
        // Only lane 0 currently written out
        
        var rxiosigs=Seq[String]() 
            for( i <-0 until tbvars.users ) { 
                 rxiosigs++=Seq("io_lanes_tx_0_bits_data_%s_udata_real".format(i))
                 rxiosigs++=Seq("io_lanes_tx_0_bits_data_%s_udata_imag".format(i))
                 // Do not plot userindexes for now
                 //rxiosigs++=Seq("io_lanes_tx_0_bits_data_%s_uindex".format(i))
             }
        var rxiotest=rxiosigs.map{ 
                            case name => "        ~$isunknown(%s) &&\n".format(name)
                            }.mkString
        rxiotest=rxiotest.patch(rxiotest.lastIndexOfSlice("&&"),"",2)

        var rxiofields =rxiosigs.map{ 
                            case name => "%d\\t"
                            }.mkString
        rxiofields=rxiofields.patch(rxiofields.lastIndexOfSlice("\\t"),"",2)

        var rxwritenames =rxiosigs.map{ 
                            case name => "        %s,\n".format(name)
                            }.mkString
        rxwritenames=rxwritenames.patch(rxwritenames.lastIndexOf(','),");",1)
         
        val rxiowrite="""|//Rx_io
                        |always @(posedge lane_clockRef ) begin 
                        |//Mimic the reading to lanes
                        |    //Print only valid values 
                        |    if ((io_lanes_tx_0_valid==1) &&  (initdone==1) && rxdone==0 &&
                        |""".stripMargin('|')+
                        rxiotest+
                        """|) begin
                        |        $fwrite(f_io_lanes_tx, """".stripMargin('|')+rxiofields+"""\n",
                        |""".stripMargin('|')+ rxwritenames +
                        """|    end
                        |    //else begin
                        |         //$display( $time, "Dropping invalid output values");
                        |    //end 
                        |end
                        """.stripMargin('|')

        // Parametrize the Tx io signal reading
        // io_lane_rx is the signal to be transmitted 
        var txioreadsigs=Seq[String]() 
            for (i <-0 until tbvars.nserdes) {
                for( k <-0 until tbvars.users ) { 
                    txioreadsigs++=Seq("    io_lanes_rx_%s_bits_data_%s_udata_real".format(i,k))
                    txioreadsigs++=Seq("    io_lanes_rx_%s_bits_data_%s_udata_imag".format(i,k))
                 }
             }
        var txioreadfields =txioreadsigs.map{ 
                            case name => "%d\\t"
                            }.mkString
        txioreadfields=txioreadfields.patch(txioreadfields.lastIndexOfSlice("\\t"),"",2)

        var txreadnames =txioreadsigs.map{ 
                            case name => "        %s,\n".format(name)
                            }.mkString
        txreadnames=txreadnames.patch(txreadnames.lastIndexOf(','),");",1)
         
        val txioread="""|     //status_io_lanes_rx=$fgets(dummyline,f_io_lanes_rx);
                        |     while (!$feof(f_io_lanes_rx)) begin
                        |             txdone<=0;
                        |             //Lane output fifo is read by the symrate clock
                        |             @(posedge io_lanes_rx_deq_clock )
                        |             status_io_lanes_rx=$fscanf(f_io_lanes_rx, """".stripMargin('|')+txioreadfields+
                        """|\n",
                        |""".stripMargin('|')+ txreadnames +
                        """|            txdone<=1;
                        |        end
                        """.stripMargin('|')

        // Parametrize the Rx io signal reading
        var rxioreadsigs=Seq[String]() 
            for (i <-0 until tbvars.rxantennas) {
                rxioreadsigs++=Seq("    io_iptr_A_%s_real".format(i))
                rxioreadsigs++=Seq("    io_iptr_A_%s_imag".format(i))
             }
        var rxioreadfields =rxioreadsigs.map{ 
                            case name => "%d\\t"
                            }.mkString
        rxioreadfields=rxioreadfields.patch(rxioreadfields.lastIndexOfSlice("\\t"),"",2)

        var rxreadnames =rxioreadsigs.map{ 
                            case name => "        %s,\n".format(name)
                            }.mkString
        rxreadnames=rxreadnames.patch(rxreadnames.lastIndexOf(','),");",1)
         
        val rxioread="""|      //status_io_lanes_rx=$fgets(dummyline,f_io_iptr_A);
                        |        while (!$feof(f_io_iptr_A)) begin
                        |            rxdone<=0;
                        |            @(posedge io_ctrl_and_clocks_adc_clocks_0 )
                        |            status_io_iptr_A=$fscanf(f_io_iptr_A, """".stripMargin('|')+rxioreadfields+
                        """|\n",
                        |""".stripMargin('|')+ rxreadnames +
                        """|           rxdone<=1;
                        |        end
                        """.stripMargin('|')

        val textTemplate=header+ extpars+pars+regdef+wiredef+assdef+iofiledef+
                        """|
                        |
                        |integer memaddrcount;
                        |integer initdone, rxdone, txdone;
                        |
                        |//Initializations
                        |initial clock = 1'b0;
                        |initial reset = 1'b0;
                        |
                        |//Clock definitions
                        |always #(c_Ts/2.0) clock = !clock ;
                        |""".stripMargin('|')+
                        txiowrite+rxiowrite+
                        """|
                        |//Clock divider model
                        |clkdiv_n_2_4_8 clkrefdiv( // @[:@3.2]
                        |  .clock(clock), // @[:@4.4]
                        |  .reset(reset_clock_div), // @[:@5.4] // used to be reset
                        |  .io_Ndiv(lane_refclk_Ndiv), // @[:@6.4]
                        |  .io_shift(lane_refclk_shift), // @[:@6.4]
                        |  .io_reset_clk(lane_refclk_reset), // @[:@6.4]
                        |  .io_clkpn (lane_clockRef), // @[:@6.4]
                        |  .io_clkp2n(clkp2n), // @[:@6.4]
                        |  .io_clkp4n(clkp4n), // @[:@6.4]
                        |  .io_clkp8n(clkp8n)// @[:@6.4]
                        |);
                        |
                        |""".stripMargin('|')+dutdef+initialdef+
                        """
                        |    #(RESET_TIME)
                        |    initdone=0;
                        |    txdone=0;
                        |    rxdone=0;
                        |    reset_clock_div=0;
                        |    io_ctrl_and_clocks_tx_reset_clkdiv=0;
                        |    io_ctrl_and_clocks_rx_reset_clkdiv=0;
                        |    lane_refclk_reset=0;
                        |    io_ctrl_and_clocks_reset_dacfifo=0;
                        |    io_ctrl_and_clocks_reset_outfifo=0;
                        |    io_ctrl_and_clocks_reset_infifo=0;
                        |    #(2*RESET_TIME)
                        |    reset=0;
                        |    #(16*RESET_TIME)
                        |    reset_loop=0;
                        |    io_ctrl_and_clocks_reset_adcfifo=0;
                        |    memaddrcount=0;
                        |//Init the LUT
                        |    
                        |    while (memaddrcount<2**9) begin
                        |       //This is really controlled by Scan, but we do not have scan model 
                        |       @(posedge clkp8n) 
                        |       io_ctrl_and_clocks_dac_lut_write_en_0<=1;
                        |       io_ctrl_and_clocks_dac_lut_write_en_1<=1;
                        |       io_ctrl_and_clocks_dac_lut_write_en_2<=1;
                        |       io_ctrl_and_clocks_dac_lut_write_en_3<=1;
                        |       io_ctrl_and_clocks_dac_lut_write_addr_0<=memaddrcount;
                        |       io_ctrl_and_clocks_dac_lut_write_addr_1<=memaddrcount;
                        |       io_ctrl_and_clocks_dac_lut_write_addr_2<=memaddrcount;
                        |       io_ctrl_and_clocks_dac_lut_write_addr_3<=memaddrcount;
                        |       io_ctrl_and_clocks_adc_lut_write_en<=1;
                        |       io_ctrl_and_clocks_adc_lut_write_addr<=memaddrcount;
                        |       if (memaddrcount < 2**8) begin
                        |          io_ctrl_and_clocks_dac_lut_write_vals_0_real<=memaddrcount+2**8; 
                        |          io_ctrl_and_clocks_dac_lut_write_vals_1_real<=memaddrcount+2**8;
                        |          io_ctrl_and_clocks_dac_lut_write_vals_2_real<=memaddrcount+2**8;
                        |          io_ctrl_and_clocks_dac_lut_write_vals_3_real<=memaddrcount+2**8;
                        |          io_ctrl_and_clocks_dac_lut_write_vals_0_imag<=memaddrcount+2**8; 
                        |          io_ctrl_and_clocks_dac_lut_write_vals_1_imag<=memaddrcount+2**8;
                        |          io_ctrl_and_clocks_dac_lut_write_vals_2_imag<=memaddrcount+2**8;
                        |          io_ctrl_and_clocks_dac_lut_write_vals_3_imag<=memaddrcount+2**8;
                        |       end
                        |
                        |       else begin
                        |          io_ctrl_and_clocks_dac_lut_write_vals_0_real<=memaddrcount-2**8; 
                        |          io_ctrl_and_clocks_dac_lut_write_vals_1_real<=memaddrcount-2**8;
                        |          io_ctrl_and_clocks_dac_lut_write_vals_2_real<=memaddrcount-2**8;
                        |          io_ctrl_and_clocks_dac_lut_write_vals_3_real<=memaddrcount-2**8;
                        |          io_ctrl_and_clocks_dac_lut_write_vals_0_imag<=memaddrcount-2**8; 
                        |          io_ctrl_and_clocks_dac_lut_write_vals_1_imag<=memaddrcount-2**8;
                        |          io_ctrl_and_clocks_dac_lut_write_vals_2_imag<=memaddrcount-2**8;
                        |          io_ctrl_and_clocks_dac_lut_write_vals_3_imag<=memaddrcount-2**8;
                        |        end  
                        |       //ADC ctrl_and_clocks_LUT
                        |       io_ctrl_and_clocks_adc_lut_write_en<=1;
                        |       io_ctrl_and_clocks_adc_lut_write_addr<=memaddrcount;
                        |       io_ctrl_and_clocks_adc_lut_write_vals_0_real<=memaddrcount; 
                        |       io_ctrl_and_clocks_adc_lut_write_vals_1_real<=memaddrcount;
                        |       io_ctrl_and_clocks_adc_lut_write_vals_2_real<=memaddrcount;
                        |       io_ctrl_and_clocks_adc_lut_write_vals_3_real<=memaddrcount;
                        |       io_ctrl_and_clocks_adc_lut_write_vals_0_imag<=memaddrcount; 
                        |       io_ctrl_and_clocks_adc_lut_write_vals_1_imag<=memaddrcount;
                        |       io_ctrl_and_clocks_adc_lut_write_vals_2_imag<=memaddrcount;
                        |       io_ctrl_and_clocks_adc_lut_write_vals_3_imag<=memaddrcount;
                        |       @(posedge clkp8n) 
                        |       memaddrcount=memaddrcount+1;
                        |       io_ctrl_and_clocks_dac_lut_write_en_0<=0;
                        |       io_ctrl_and_clocks_dac_lut_write_en_1<=0;
                        |       io_ctrl_and_clocks_dac_lut_write_en_2<=0;
                        |       io_ctrl_and_clocks_dac_lut_write_en_3<=0;
                        |       io_ctrl_and_clocks_adc_lut_write_en<=0;
                        |    end
                        |    io_ctrl_and_clocks_adc_lut_reset<=0;
                        |    initdone<=1;
                        |    fork
                        |""".stripMargin('|')+txioread+rxioread+
                        """
                        |    join
                        |""".stripMargin('|')+iofileclose+
                        """|
                        |    $finish;
                        |end
                        |endmodule""".stripMargin('|')
        //val testbench=Handlebars(textTemplate)
        //tb write testbench(tbvars)
        tb write textTemplate
        tb.close()
  }
}

