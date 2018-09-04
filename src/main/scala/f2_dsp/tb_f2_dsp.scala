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
             val uindexbits=2
             val rxindexbits=2
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
            val ioseq=Seq( 
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
                          ("in","io_iptr_A_0_real",rx.inbits-1,0,"None","'b0"),
                          ("in","io_iptr_A_0_imag",rx.inbits-1,0,"None","'b0"),
                          ("in","io_iptr_A_1_real",rx.inbits-1,0,"None","'b0"),
                          ("in","io_iptr_A_1_imag",rx.inbits-1,0,"None","'b0"),
                          ("in","io_iptr_A_2_real",rx.inbits-1,0,"None","'b0"),
                          ("in","io_iptr_A_2_imag",rx.inbits-1,0,"None","'b0"),
                          ("in","io_iptr_A_3_real",rx.inbits-1,0,"None","'b0"),
                          ("in","io_iptr_A_3_imag",rx.inbits-1,0,"None","'b0"),
                          ("in","io_ctrl_and_clocks_interpolator_controls_0_cic3derivscale",tx.scalebits-1,0,"None","g_tx_scale3"),
                          ("in","io_ctrl_and_clocks_interpolator_controls_0_cic3derivshift",tx.derivshiftbits-1,0,"None","g_tx_cic3shift"),
                          ("in","io_ctrl_and_clocks_interpolator_controls_0_hb1scale",tx.scalebits-1,0,"None","g_tx_scale0"),
                          ("in","io_ctrl_and_clocks_interpolator_controls_0_hb2scale",tx.scalebits-1,0,"None","g_tx_scale1"),
                          ("in","io_ctrl_and_clocks_interpolator_controls_0_hb3scale",tx.scalebits-1,0,"None","g_tx_scale2"),
                          ("in","io_ctrl_and_clocks_interpolator_controls_0_mode",tx.interpmodebits-1,0,"None","g_tx_interpolator_mode"),
                          ("in","io_ctrl_and_clocks_interpolator_controls_0_reset_loop","None","None","reset_loop","None"),
                          ("in","io_ctrl_and_clocks_interpolator_controls_1_cic4derivscale",tx.scalebits-1,0,"None","g_tx_scale3"),
                          ("in","io_ctrl_and_clocks_interpolator_controls_1_cic3derivshift",tx.derivshiftbits-1,0,"None","g_tx_cic3shift"),
                          ("in","io_ctrl_and_clocks_interpolator_controls_1_hb1scale",tx.scalebits-1,0,"None","g_tx_scale0"),
                          ("in","io_ctrl_and_clocks_interpolator_controls_1_hb2scale",tx.scalebits-1,0,"None","g_tx_scale1"),
                          ("in","io_ctrl_and_clocks_interpolator_controls_1_hb3scale",tx.scalebits-1,0,"None","g_tx_scale2"),
                          ("in","io_ctrl_and_clocks_interpolator_controls_1_mode",tx.interpmodebits-1,0,"None","g_tx_interpolator_mode"),
                          ("in","io_ctrl_and_clocks_interpolator_controls_1_reset_loop","None","None","reset_loop","None"),
                          ("in","io_ctrl_and_clocks_interpolator_controls_2_cic3derivscale",tx.scalebits-1,0,"None","g_tx_scale3"),
                          ("in","io_ctrl_and_clocks_interpolator_controls_2_cic3derivshift",tx.derivshiftbits-1,0,"None","g_tx_cic3shift"),
                          ("in","io_ctrl_and_clocks_interpolator_controls_2_hb1scale",tx.scalebits-1,0,"None","g_tx_scale0"),
                          ("in","io_ctrl_and_clocks_interpolator_controls_2_hb2scale",tx.scalebits-1,0,"None","g_tx_scale1"),
                          ("in","io_ctrl_and_clocks_interpolator_controls_2_hb3scale",tx.scalebits-1,0,"None","g_tx_scale2"),
                          ("in","io_ctrl_and_clocks_interpolator_controls_2_mode",tx.interpmodebits-1,0,"None","g_tx_interpolator_mode"),
                          ("in","io_ctrl_and_clocks_interpolator_controls_2_reset_loop","None","None","reset_loop","None"),
                          ("in","io_ctrl_and_clocks_interpolator_controls_3_cic3derivscale",tx.scalebits-1,0,"None","g_tx_scale3"),
                          ("in","io_ctrl_and_clocks_interpolator_controls_3_cic3derivshift",tx.derivshiftbits-1,0,"None","g_tx_cic3shift"),
                          ("in","io_ctrl_and_clocks_interpolator_controls_3_hb1scale",tx.scalebits-1,0,"None","g_tx_scale0"),
                          ("in","io_ctrl_and_clocks_interpolator_controls_3_hb2scale",tx.scalebits-1,0,"None","g_tx_scale1"),
                          ("in","io_ctrl_and_clocks_interpolator_controls_3_hb3scale",tx.scalebits-1,0,"None","g_tx_scale2"),
                          ("in","io_ctrl_and_clocks_interpolator_controls_3_mode",tx.interpmodebits-1,0,"None","g_tx_interpolator_mode"),
                          ("in","io_ctrl_and_clocks_interpolator_controls_3_reset_loop","None","None","reset_loop","None"),
                          ("dclk","io_ctrl_and_clocks_dac_clocks_0","None","None","clock","None"),
                          ("dclk","io_ctrl_and_clocks_dac_clocks_1","None","None","clock","None"),
                          ("dclk","io_ctrl_and_clocks_dac_clocks_2","None","None","clock","None"),
                          ("dclk","io_ctrl_and_clocks_dac_clocks_3","None","None","clock","None"),
                          ("in","io_ctrl_and_clocks_reset_dacfifo","None","None","None","'b1"),
                          ("in","io_ctrl_and_clocks_user_spread_mode",tx.spreadmodebits-1,0,"None","g_tx_user_spread_mode"),
                          ("in","io_ctrl_and_clocks_user_sum_mode_0",tx.summodebits-1,0,"None","g_tx_user_sum_mode"),
                          ("in","io_ctrl_and_clocks_user_sum_mode_1",tx.summodebits-1,0,"None","g_tx_user_sum_mode"),
                          ("in","io_ctrl_and_clocks_user_sum_mode_2",tx.summodebits-1,0,"None","g_tx_user_sum_mode"),
                          ("in","io_ctrl_and_clocks_user_sum_mode_3",tx.summodebits-1,0,"None","g_tx_user_sum_mode"),
                          ("in","io_ctrl_and_clocks_user_select_index_0",uindexbits-1,0,"None","g_tx_user_select_index"),
                          ("in","io_ctrl_and_clocks_user_select_index_1",uindexbits-1,0,"None","g_tx_user_select_index"),
                          ("in","io_ctrl_and_clocks_user_select_index_2",uindexbits-1,0,"None","g_tx_user_select_index"),
                          ("in","io_ctrl_and_clocks_user_select_index_3",uindexbits-1,0,"None","g_tx_user_select_index"),
                          ("in","io_ctrl_and_clocks_dac_data_mode_0",tx.dacdatamodebits-1,0,"None","g_tx_dac_data_mode"),
                          ("in","io_ctrl_and_clocks_dac_data_mode_1",tx.dacdatamodebits-1,0,"None","g_tx_dac_data_mode"),
                          ("in","io_ctrl_and_clocks_dac_data_mode_2",tx.dacdatamodebits-1,0,"None","g_tx_dac_data_mode"),
                          ("in","io_ctrl_and_clocks_dac_data_mode_3",tx.dacdatamodebits-1,0,"None","g_tx_dac_data_mode"),
                          ("in","io_ctrl_and_clocks_dac_lut_write_addr_0",tx.outbits-1,0,"None",0),
                          ("in","io_ctrl_and_clocks_dac_lut_write_addr_1",tx.outbits-1,0,"None",0),
                          ("in","io_ctrl_and_clocks_dac_lut_write_addr_2",tx.outbits-1,0,"None",0),
                          ("in","io_ctrl_and_clocks_dac_lut_write_addr_3",tx.outbits-1,0,"None",0),
                          ("in","io_ctrl_and_clocks_dac_lut_write_vals_0_real",tx.outbits-1,0,"None",0),
                          ("in","io_ctrl_and_clocks_dac_lut_write_vals_0_imag",tx.outbits-1,0,"None",0),
                          ("in","io_ctrl_and_clocks_dac_lut_write_vals_1_real",tx.outbits-1,0,"None",0),
                          ("in","io_ctrl_and_clocks_dac_lut_write_vals_1_imag",tx.outbits-1,0,"None",0),
                          ("in","io_ctrl_and_clocks_dac_lut_write_vals_2_real",tx.outbits-1,0,"None",0),
                          ("in","io_ctrl_and_clocks_dac_lut_write_vals_2_imag",tx.outbits-1,0,"None",0),
                          ("in","io_ctrl_and_clocks_dac_lut_write_vals_3_real",tx.outbits-1,0,"None",0),
                          ("in","io_ctrl_and_clocks_dac_lut_write_vals_3_imag",tx.outbits-1,0,"None",0),
                          ("in","io_ctrl_and_clocks_dac_lut_write_en_0","None","None","None",0),
                          ("in","io_ctrl_and_clocks_dac_lut_write_en_1","None","None","None",0),
                          ("in","io_ctrl_and_clocks_dac_lut_write_en_2","None","None","None",0),
                          ("in","io_ctrl_and_clocks_dac_lut_write_en_3","None","None","None",0),
                          // In SerDes, TX is a input for the transmitter, RX is the output of the receiver
                          // Thus, lanes_tx is an output, lanes_rx is an input
                          ("out","io_lanes_tx_deq_clock","None","None","None","None"),
                          ("in","io_lanes_tx_0_ready","None","None","None","'b1"),
                          ("out","io_lanes_tx_0_valid","None","None","None","None"),
                          ("outs","io_lanes_tx_0_bits_data_0_udata_real",tx.inbits-1,0,"None","None"),
                          ("outs","io_lanes_tx_0_bits_data_0_udata_imag",tx.inbits-1,0,"None","None"),
                          ("out","io_lanes_tx_0_bits_data_0_uindex",uindexbits-1,0,"None","None"),
                          ("outs","io_lanes_tx_0_bits_data_1_udata_real",tx.inbits-1,0,"None","None"),
                          ("outs","io_lanes_tx_0_bits_data_1_udata_imag",tx.inbits-1,0,"None","None"),
                          ("out","io_lanes_tx_0_bits_data_1_uindex",uindexbits-1,0,"None","None"),
                          ("outs","io_lanes_tx_0_bits_data_2_udata_real",tx.inbits-1,0,"None","None"),
                          ("outs","io_lanes_tx_0_bits_data_2_udata_imag",tx.inbits-1,0,"None","None"),
                          ("out","io_lanes_tx_0_bits_data_2_uindex",uindexbits-1,0,"None","None"),
                          ("outs","io_lanes_tx_0_bits_data_3_udata_real",tx.inbits-1,0,"None","None"),
                          ("outs","io_lanes_tx_0_bits_data_3_udata_imag",tx.inbits-1,0,"None","None"),
                          ("out","io_lanes_tx_0_bits_data_3_uindex",uindexbits-1,0,"None","None"),
                          ("out","io_lanes_tx_0_bits_rxindex",rxindexbits-1,0,"None","None"),
                          ("in","io_lanes_tx_1_ready","None","None","None","'b1"),
                          ("out","io_lanes_tx_1_valid","None","None","None","None"),
                          ("outs","io_lanes_tx_1_bits_data_0_udata_real",tx.inbits-1,0,"None","None"),
                          ("outs","io_lanes_tx_1_bits_data_0_udata_imag",tx.inbits-1,0,"None","None"),
                          ("out","io_lanes_tx_1_bits_data_0_uindex",uindexbits-1,0,"None","None"),
                          ("outs","io_lanes_tx_1_bits_data_1_udata_real",tx.inbits-1,0,"None","None"),
                          ("outs","io_lanes_tx_1_bits_data_1_udata_imag",tx.inbits-1,0,"None","None"),
                          ("out","io_lanes_tx_1_bits_data_1_uindex",uindexbits-1,0,"None","None"),
                          ("outs","io_lanes_tx_1_bits_data_2_udata_real",tx.inbits-1,0,"None","None"),
                          ("outs","io_lanes_tx_1_bits_data_2_udata_imag",tx.inbits-1,0,"None","None"),
                          ("out","io_lanes_tx_1_bits_data_2_uindex",uindexbits-1,0,"None","None"),
                          ("outs","io_lanes_tx_1_bits_data_3_udata_real",tx.inbits-1,0,"None","None"),
                          ("outs","io_lanes_tx_1_bits_data_3_udata_imag",tx.inbits-1,0,"None","None"),
                          ("out","io_lanes_tx_1_bits_data_3_uindex",uindexbits-1,0,"None","None"),
                          ("out","io_lanes_tx_1_bits_rxindex",rxindexbits-1,0,"None","None"),
                          ("out","io_lanes_rx_0_ready","None","None","None","None"),
                          ("in","io_lanes_rx_0_valid","None","None","None","'b1"),
                          ("in","io_lanes_rx_0_bits_data_0_udata_real",tx.inbits-1,0,"None","'b0"),
                          ("in","io_lanes_rx_0_bits_data_0_udata_imag",tx.inbits-1,0,"None","'b0"),
                          ("in","io_lanes_rx_0_bits_data_0_uindex",uindexbits-1,0,"None","'b0"),
                          ("in","io_lanes_rx_0_bits_data_1_udata_real",tx.inbits-1,0,"None","'b0"),
                          ("in","io_lanes_rx_0_bits_data_1_udata_imag",tx.inbits-1,0,"None","'b0"),
                          ("in","io_lanes_rx_0_bits_data_1_uindex",uindexbits-1,0,"None","'b0"),
                          ("in","io_lanes_rx_0_bits_data_2_udata_real",tx.inbits-1,0,"None","'b0"),
                          ("in","io_lanes_rx_0_bits_data_2_udata_imag",tx.inbits-1,0,"None","'b0"),
                          ("in","io_lanes_rx_0_bits_data_2_uindex",uindexbits-1,0,"None","'b0"),
                          ("in","io_lanes_rx_0_bits_data_3_udata_real",tx.inbits-1,0,"None","'b0"),
                          ("in","io_lanes_rx_0_bits_data_3_udata_imag",tx.inbits-1,0,"None","'b0"),
                          ("in","io_lanes_rx_0_bits_data_3_uindex",uindexbits-1,0,"None","'b0"),
                          ("in","io_lanes_rx_0_bits_rxindex",rxindexbits-1,0,"None","'b0"),
                          ("out","io_lanes_rx_1_ready","None","None","None","None"),
                          ("in","io_lanes_rx_1_valid","None","None","None","'b1"),
                          ("in","io_lanes_rx_1_bits_data_0_udata_real",tx.inbits-1,0,"None","'b0"),
                          ("in","io_lanes_rx_1_bits_data_0_udata_imag",tx.inbits-1,0,"None","'b0"),
                          ("in","io_lanes_rx_1_bits_data_0_uindex",uindexbits-1,0,"None","'b0"),
                          ("in","io_lanes_rx_1_bits_data_1_udata_real",tx.inbits-1,0,"None","'b0"),
                          ("in","io_lanes_rx_1_bits_data_1_udata_imag",tx.inbits-1,0,"None","'b0"),
                          ("in","io_lanes_rx_1_bits_data_1_uindex",uindexbits-1,0,"None","'b0"),
                          ("in","io_lanes_rx_1_bits_data_2_udata_real",tx.inbits-1,0,"None","'b0"),
                          ("in","io_lanes_rx_1_bits_data_2_udata_imag",tx.inbits-1,0,"None","'b0"),
                          ("in","io_lanes_rx_1_bits_data_2_uindex",uindexbits-1,0,"None","'b0"),
                          ("in","io_lanes_rx_1_bits_data_3_udata_real",tx.inbits-1,0,"None","'b0"),
                          ("in","io_lanes_rx_1_bits_data_3_udata_imag",tx.inbits-1,0,"None","'b0"),
                          ("in","io_lanes_rx_1_bits_data_3_uindex",uindexbits-1,0,"None","'b0"),
                          ("in","io_lanes_rx_1_bits_rxindex",rxindexbits-1,0,"None","'b0"),
                          ("in","io_ctrl_and_clocks_tx_user_delays_0_0",tx.txuserdelaybits-1,0,"None",0),
                          ("in","io_ctrl_and_clocks_tx_user_delays_0_1",tx.txuserdelaybits-1,0,"None",0),
                          ("in","io_ctrl_and_clocks_tx_user_delays_0_2",tx.txuserdelaybits-1,0,"None",0),
                          ("in","io_ctrl_and_clocks_tx_user_delays_0_3",tx.txuserdelaybits-1,0,"None",0),
                          ("in","io_ctrl_and_clocks_tx_user_delays_1_0",tx.txuserdelaybits-1,0,"None",0),
                          ("in","io_ctrl_and_clocks_tx_user_delays_1_1",tx.txuserdelaybits-1,0,"None",0),
                          ("in","io_ctrl_and_clocks_tx_user_delays_1_2",tx.txuserdelaybits-1,0,"None",0),
                          ("in","io_ctrl_and_clocks_tx_user_delays_1_3",tx.txuserdelaybits-1,0,"None",0),
                          ("in","io_ctrl_and_clocks_tx_user_delays_2_0",tx.txuserdelaybits-1,0,"None",0),
                          ("in","io_ctrl_and_clocks_tx_user_delays_2_1",tx.txuserdelaybits-1,0,"None",0),
                          ("in","io_ctrl_and_clocks_tx_user_delays_2_2",tx.txuserdelaybits-1,0,"None",0),
                          ("in","io_ctrl_and_clocks_tx_user_delays_2_3",tx.txuserdelaybits-1,0,"None",0),
                          ("in","io_ctrl_and_clocks_tx_user_delays_3_0",tx.txuserdelaybits-1,0,"None",0),
                          ("in","io_ctrl_and_clocks_tx_user_delays_3_1",tx.txuserdelaybits-1,0,"None",0),
                          ("in","io_ctrl_and_clocks_tx_user_delays_3_2",tx.txuserdelaybits-1,0,"None",0),
                          ("in","io_ctrl_and_clocks_tx_user_delays_3_3",tx.txuserdelaybits-1,0,"None",0),
                          ("in","io_ctrl_and_clocks_tx_fine_delays_0",tx.txfinedelaybits-1,0,"None",0),
                          ("in","io_ctrl_and_clocks_tx_fine_delays_1",tx.txfinedelaybits-1,0,"None",0),
                          ("in","io_ctrl_and_clocks_tx_fine_delays_2",tx.txfinedelaybits-1,0,"None",0),
                          ("in","io_ctrl_and_clocks_tx_fine_delays_3",tx.txfinedelaybits-1,0,"None",0),
                          ("in","io_ctrl_and_clocks_tx_user_weights_0_0_real",tx.txweightbits-1,0,"None",1),
                          ("in","io_ctrl_and_clocks_tx_user_weights_0_0_imag",tx.txweightbits-1,0,"None",1),
                          ("in","io_ctrl_and_clocks_tx_user_weights_0_1_real",tx.txweightbits-1,0,"None",1),
                          ("in","io_ctrl_and_clocks_tx_user_weights_0_1_imag",tx.txweightbits-1,0,"None",1),
                          ("in","io_ctrl_and_clocks_tx_user_weights_0_2_real",tx.txweightbits-1,0,"None",1),
                          ("in","io_ctrl_and_clocks_tx_user_weights_0_2_imag",tx.txweightbits-1,0,"None",1),
                          ("in","io_ctrl_and_clocks_tx_user_weights_0_3_real",tx.txweightbits-1,0,"None",1),
                          ("in","io_ctrl_and_clocks_tx_user_weights_0_3_imag",tx.txweightbits-1,0,"None",1),
                          ("in","io_ctrl_and_clocks_tx_user_weights_1_0_real",tx.txweightbits-1,0,"None",1),
                          ("in","io_ctrl_and_clocks_tx_user_weights_1_0_imag",tx.txweightbits-1,0,"None",1),
                          ("in","io_ctrl_and_clocks_tx_user_weights_1_1_real",tx.txweightbits-1,0,"None",1),
                          ("in","io_ctrl_and_clocks_tx_user_weights_1_1_imag",tx.txweightbits-1,0,"None",1),
                          ("in","io_ctrl_and_clocks_tx_user_weights_1_2_real",tx.txweightbits-1,0,"None",1),
                          ("in","io_ctrl_and_clocks_tx_user_weights_1_2_imag",tx.txweightbits-1,0,"None",1),
                          ("in","io_ctrl_and_clocks_tx_user_weights_1_3_real",tx.txweightbits-1,0,"None",1),
                          ("in","io_ctrl_and_clocks_tx_user_weights_1_3_imag",tx.txweightbits-1,0,"None",1),
                          ("in","io_ctrl_and_clocks_tx_user_weights_2_0_real",tx.txweightbits-1,0,"None",1),
                          ("in","io_ctrl_and_clocks_tx_user_weights_2_0_imag",tx.txweightbits-1,0,"None",1),
                          ("in","io_ctrl_and_clocks_tx_user_weights_2_1_real",tx.txweightbits-1,0,"None",1),
                          ("in","io_ctrl_and_clocks_tx_user_weights_2_1_imag",tx.txweightbits-1,0,"None",1),
                          ("in","io_ctrl_and_clocks_tx_user_weights_2_2_real",tx.txweightbits-1,0,"None",1),
                          ("in","io_ctrl_and_clocks_tx_user_weights_2_2_imag",tx.txweightbits-1,0,"None",1),
                          ("in","io_ctrl_and_clocks_tx_user_weights_2_3_real",tx.txweightbits-1,0,"None",1),
                          ("in","io_ctrl_and_clocks_tx_user_weights_2_3_imag",tx.txweightbits-1,0,"None",1),
                          ("in","io_ctrl_and_clocks_tx_user_weights_3_0_real",tx.txweightbits-1,0,"None",1),
                          ("in","io_ctrl_and_clocks_tx_user_weights_3_0_imag",tx.txweightbits-1,0,"None",1),
                          ("in","io_ctrl_and_clocks_tx_user_weights_3_1_real",tx.txweightbits-1,0,"None",1),
                          ("in","io_ctrl_and_clocks_tx_user_weights_3_1_imag",tx.txweightbits-1,0,"None",1),
                          ("in","io_ctrl_and_clocks_tx_user_weights_3_2_real",tx.txweightbits-1,0,"None",1),
                          ("in","io_ctrl_and_clocks_tx_user_weights_3_2_imag",tx.txweightbits-1,0,"None",1),
                          ("in","io_ctrl_and_clocks_tx_user_weights_3_3_real",tx.txweightbits-1,0,"None",1),
                          ("in","io_ctrl_and_clocks_tx_user_weights_3_3_imag",tx.txweightbits-1,0,"None",1),
                          ("out","io_Z_0_real_b",tx.bin-1,0,"None","None"),
                          ("out","io_Z_0_real_t",scala.math.pow(2,tx.thermo).toInt-2,0,"None","None"),
                          ("out","io_Z_0_imag_b",tx.bin-1,0,"None","None"),
                          ("out","io_Z_0_imag_t",scala.math.pow(2,tx.thermo).toInt-2,0,"None","None"),
                          ("out","io_Z_1_real_b",tx.bin-1,0,"None","None"),
                          ("out","io_Z_1_real_t",scala.math.pow(2,tx.thermo).toInt-2,0,"None","None"),
                          ("out","io_Z_1_imag_b",tx.bin-1,0,"None","None"),
                          ("out","io_Z_1_imag_t",scala.math.pow(2,tx.thermo).toInt-2,0,"None","None"),
                          ("out","io_Z_2_real_b",tx.bin-1,0,"None","None"),
                          ("out","io_Z_2_real_t",scala.math.pow(2,tx.thermo).toInt-2,0,"None","None"),
                          ("out","io_Z_2_imag_b",tx.bin-1,0,"None","None"),
                          ("out","io_Z_2_imag_t",scala.math.pow(2,tx.thermo).toInt-2,0,"None","None"),
                          ("out","io_Z_3_real_b",tx.bin-1,0,"None","None"),
                          ("out","io_Z_3_real_t",scala.math.pow(2,tx.thermo).toInt-2,0,"None","None"),
                          ("out","io_Z_3_imag_b",tx.bin-1,0,"None","None"),
                          ("out","io_Z_3_imag_t",scala.math.pow(2,tx.thermo).toInt-2,0,"None","None"),
                          //RX starts here
                          ("in","io_ctrl_and_clocks_decimator_controls_0_cic3integscale",rx.gainbits-1,0,"None","g_rx_scale0"),
                          ("in","io_ctrl_and_clocks_decimator_controls_0_cic3integshift",rx.integshiftbits-1,0,"None","g_tx_cic3shift"),
                          ("in","io_ctrl_and_clocks_decimator_controls_0_hb1scale",rx.gainbits-1,0,"None","g_rx_scale1"),
                          ("in","io_ctrl_and_clocks_decimator_controls_0_hb2scale",rx.gainbits-1,0,"None","g_rx_scale2"),
                          ("in","io_ctrl_and_clocks_decimator_controls_0_hb3scale",rx.gainbits-1,0,"None","g_rx_scale3"),
                          ("in","io_ctrl_and_clocks_decimator_controls_0_mode",rx.decimator_modebits-1,0,"None","g_rx_mode"),
                          ("in","io_ctrl_and_clocks_decimator_controls_1_cic3integscale",rx.gainbits-1,0,"None","g_rx_scale0"),
                          ("in","io_ctrl_and_clocks_decimator_controls_1_cic3integshift",rx.integshiftbits-1,0,"None","g_tx_cic3shift"),
                          ("in","io_ctrl_and_clocks_decimator_controls_1_hb1scale",rx.gainbits-1,0,"None","g_rx_scale1"),
                          ("in","io_ctrl_and_clocks_decimator_controls_1_hb2scale",rx.gainbits-1,0,"None","g_rx_scale2"),
                          ("in","io_ctrl_and_clocks_decimator_controls_1_hb3scale",rx.gainbits-1,0,"None","g_rx_scale3"),
                          ("in","io_ctrl_and_clocks_decimator_controls_1_mode",rx.decimator_modebits-1,0,"None","g_rx_mode"),
                          ("in","io_ctrl_and_clocks_decimator_controls_2_cic3integscale",rx.gainbits-1,0,"None","g_rx_scale0"),
                          ("in","io_ctrl_and_clocks_decimator_controls_2_cic3integshift",rx.integshiftbits-1,0,"None","g_tx_cic3shift"),
                          ("in","io_ctrl_and_clocks_decimator_controls_2_hb1scale",rx.gainbits-1,0,"None","g_rx_scale1"),
                          ("in","io_ctrl_and_clocks_decimator_controls_2_hb2scale",rx.gainbits-1,0,"None","g_rx_scale2"),
                          ("in","io_ctrl_and_clocks_decimator_controls_2_hb3scale",rx.gainbits-1,0,"None","g_rx_scale3"),
                          ("in","io_ctrl_and_clocks_decimator_controls_2_mode",rx.decimator_modebits-1,0,"None","g_rx_mode"),
                          ("in","io_ctrl_and_clocks_decimator_controls_3_cic3integscale",rx.gainbits-1,0,"None","g_rx_scale0"),
                          ("in","io_ctrl_and_clocks_decimator_controls_3_cic3integshift",rx.integshiftbits-1,0,"None","g_tx_cic3shift"),
                          ("in","io_ctrl_and_clocks_decimator_controls_3_hb1scale",rx.gainbits-1,0,"None","g_rx_scale1"),
                          ("in","io_ctrl_and_clocks_decimator_controls_3_hb2scale",rx.gainbits-1,0,"None","g_rx_scale2"),
                          ("in","io_ctrl_and_clocks_decimator_controls_3_hb3scale",rx.gainbits-1,0,"None","g_rx_scale3"),
                          ("in","io_ctrl_and_clocks_decimator_controls_3_mode",rx.decimator_modebits-1,0,"None","g_rx_mode"),
                          ("dclk","io_ctrl_and_clocks_adc_clocks_0","None","None","clock","None"),
                          ("dclk","io_ctrl_and_clocks_adc_clocks_1","None","None","clock","None"),
                          ("dclk","io_ctrl_and_clocks_adc_clocks_2","None","None","clock","None"),
                          ("dclk","io_ctrl_and_clocks_adc_clocks_3","None","None","clock","None"),
                          ("in","io_ctrl_and_clocks_user_index",uindexbits-1,0,"None","g_rx_user_index"),
                          ("in","io_ctrl_and_clocks_antenna_index",rxindexbits-1,0,"None","g_rx_antenna_index"),
                          ("in","io_ctrl_and_clocks_reset_index_count","None","None","None",1),
                          ("in","io_ctrl_and_clocks_reset_outfifo","None","None","None",1),
                          ("in","io_ctrl_and_clocks_reset_adcfifo","None","None","None",1),
                          ("in","io_ctrl_and_clocks_reset_infifo","None","None","None",1),
                          ("in","io_ctrl_and_clocks_rx_output_mode",rx.rx_output_modebits-1,0,"None","g_rx_output_mode"),
                          ("in","io_ctrl_and_clocks_input_mode",rx.input_modebits-1,0,"None","g_rx_input_mode"),
                          ("in","io_ctrl_and_clocks_adc_fifo_lut_mode",rx.adc_fifo_lut_modebits-1,0,"None","g_rx_adc_fifo_lut_mode"),
                          ("in","io_ctrl_and_clocks_inv_adc_clk_pol_0","None","None","None","g_rx_inv_adc_clk_pol"),
                          ("in","io_ctrl_and_clocks_inv_adc_clk_pol_1","None","None","None","g_rx_inv_adc_clk_pol"),
                          ("in","io_ctrl_and_clocks_inv_adc_clk_pol_2","None","None","None","g_rx_inv_adc_clk_pol"),
                          ("in","io_ctrl_and_clocks_inv_adc_clk_pol_3","None","None","None","g_rx_inv_adc_clk_pol"),
                          ("in","io_ctrl_and_clocks_adc_lut_write_addr",rx.adc_lut_width-1,0,"None",0),
                          ("in","io_ctrl_and_clocks_adc_lut_write_vals_0_real",rx.adc_lut_width-1,0,"None",0),
                          ("in","io_ctrl_and_clocks_adc_lut_write_vals_1_real",rx.adc_lut_width-1,0,"None",0),
                          ("in","io_ctrl_and_clocks_adc_lut_write_vals_2_real",rx.adc_lut_width-1,0,"None",0),
                          ("in","io_ctrl_and_clocks_adc_lut_write_vals_3_real",rx.adc_lut_width-1,0,"None",0),
                          ("in","io_ctrl_and_clocks_adc_lut_write_vals_0_imag",rx.adc_lut_width-1,0,"None",0),
                          ("in","io_ctrl_and_clocks_adc_lut_write_vals_1_imag",rx.adc_lut_width-1,0,"None",0),
                          ("in","io_ctrl_and_clocks_adc_lut_write_vals_2_imag",rx.adc_lut_width-1,0,"None",0),
                          ("in","io_ctrl_and_clocks_adc_lut_write_vals_3_imag",rx.adc_lut_width-1,0,"None",0),
                          ("in","io_ctrl_and_clocks_adc_lut_write_en","None","None","None",0),
                          ("in","io_ctrl_and_clocks_adc_lut_reset","None","None","None",1),

                          // user_delays
                          ("in","io_ctrl_and_clocks_rx_user_delays_0_0",rx.delay_width-1,0,"None","'b0"),
                          ("in","io_ctrl_and_clocks_rx_user_delays_0_1",rx.delay_width-1,0,"None","'b0"),
                          ("in","io_ctrl_and_clocks_rx_user_delays_0_2",rx.delay_width-1,0,"None","'b0"),
                          ("in","io_ctrl_and_clocks_rx_user_delays_0_3",rx.delay_width-1,0,"None","'b0"),
                          ("in","io_ctrl_and_clocks_rx_user_delays_1_0",rx.delay_width-1,0,"None","'b0"),
                          ("in","io_ctrl_and_clocks_rx_user_delays_1_1",rx.delay_width-1,0,"None","'b0"),
                          ("in","io_ctrl_and_clocks_rx_user_delays_1_2",rx.delay_width-1,0,"None","'b0"),
                          ("in","io_ctrl_and_clocks_rx_user_delays_1_3",rx.delay_width-1,0,"None","'b0"),
                          ("in","io_ctrl_and_clocks_rx_user_delays_2_0",rx.delay_width-1,0,"None","'b0"),
                          ("in","io_ctrl_and_clocks_rx_user_delays_2_1",rx.delay_width-1,0,"None","'b0"),
                          ("in","io_ctrl_and_clocks_rx_user_delays_2_2",rx.delay_width-1,0,"None","'b0"),
                          ("in","io_ctrl_and_clocks_rx_user_delays_2_3",rx.delay_width-1,0,"None","'b0"),
                          ("in","io_ctrl_and_clocks_rx_user_delays_3_0",rx.delay_width-1,0,"None","'b0"),
                          ("in","io_ctrl_and_clocks_rx_user_delays_3_1",rx.delay_width-1,0,"None","'b0"),
                          ("in","io_ctrl_and_clocks_rx_user_delays_3_2",rx.delay_width-1,0,"None","'b0"),
                          ("in","io_ctrl_and_clocks_rx_user_delays_3_3",rx.delay_width-1,0,"None","'b0"),

                          // fine_delays
                          ("in","io_ctrl_and_clocks_rx_fine_delays_0",rx.fine_delay_width-1,0,"None","'b0"),
                          ("in","io_ctrl_and_clocks_rx_fine_delays_1",rx.fine_delay_width-1,0,"None","'b0"),
                          ("in","io_ctrl_and_clocks_rx_fine_delays_2",rx.fine_delay_width-1,0,"None","'b0"),
                          ("in","io_ctrl_and_clocks_rx_fine_delays_3",rx.fine_delay_width-1,0,"None","'b0"),

                          // user_weights
                          ("in","io_ctrl_and_clocks_rx_user_weights_0_0_real",rx.weight_width-1,0,"None","'b1"),
                          ("in","io_ctrl_and_clocks_rx_user_weights_0_0_imag",rx.weight_width-1,0,"None","'b1"),
                          ("in","io_ctrl_and_clocks_rx_user_weights_0_1_real",rx.weight_width-1,0,"None","'b1"),
                          ("in","io_ctrl_and_clocks_rx_user_weights_0_1_imag",rx.weight_width-1,0,"None","'b1"),
                          ("in","io_ctrl_and_clocks_rx_user_weights_0_2_real",rx.weight_width-1,0,"None","'b1"),
                          ("in","io_ctrl_and_clocks_rx_user_weights_0_2_imag",rx.weight_width-1,0,"None","'b1"),
                          ("in","io_ctrl_and_clocks_rx_user_weights_0_3_real",rx.weight_width-1,0,"None","'b1"),
                          ("in","io_ctrl_and_clocks_rx_user_weights_0_3_imag",rx.weight_width-1,0,"None","'b1"),
                          ("in","io_ctrl_and_clocks_rx_user_weights_1_0_real",rx.weight_width-1,0,"None","'b1"),
                          ("in","io_ctrl_and_clocks_rx_user_weights_1_0_imag",rx.weight_width-1,0,"None","'b1"),
                          ("in","io_ctrl_and_clocks_rx_user_weights_1_1_real",rx.weight_width-1,0,"None","'b1"),
                          ("in","io_ctrl_and_clocks_rx_user_weights_1_1_imag",rx.weight_width-1,0,"None","'b1"),
                          ("in","io_ctrl_and_clocks_rx_user_weights_1_2_real",rx.weight_width-1,0,"None","'b1"),
                          ("in","io_ctrl_and_clocks_rx_user_weights_1_2_imag",rx.weight_width-1,0,"None","'b1"),
                          ("in","io_ctrl_and_clocks_rx_user_weights_1_3_real",rx.weight_width-1,0,"None","'b1"),
                          ("in","io_ctrl_and_clocks_rx_user_weights_1_3_imag",rx.weight_width-1,0,"None","'b1"),
                          ("in","io_ctrl_and_clocks_rx_user_weights_2_0_real",rx.weight_width-1,0,"None","'b1"),
                          ("in","io_ctrl_and_clocks_rx_user_weights_2_0_imag",rx.weight_width-1,0,"None","'b1"),
                          ("in","io_ctrl_and_clocks_rx_user_weights_2_1_real",rx.weight_width-1,0,"None","'b1"),
                          ("in","io_ctrl_and_clocks_rx_user_weights_2_1_imag",rx.weight_width-1,0,"None","'b1"),
                          ("in","io_ctrl_and_clocks_rx_user_weights_2_2_real",rx.weight_width-1,0,"None","'b1"),
                          ("in","io_ctrl_and_clocks_rx_user_weights_2_2_imag",rx.weight_width-1,0,"None","'b1"),
                          ("in","io_ctrl_and_clocks_rx_user_weights_2_3_real",rx.weight_width-1,0,"None","'b1"),
                          ("in","io_ctrl_and_clocks_rx_user_weights_2_3_imag",rx.weight_width-1,0,"None","'b1"),
                          ("in","io_ctrl_and_clocks_rx_user_weights_3_0_real",rx.weight_width-1,0,"None","'b1"),
                          ("in","io_ctrl_and_clocks_rx_user_weights_3_0_imag",rx.weight_width-1,0,"None","'b1"),
                          ("in","io_ctrl_and_clocks_rx_user_weights_3_1_real",rx.weight_width-1,0,"None","'b1"),
                          ("in","io_ctrl_and_clocks_rx_user_weights_3_1_imag",rx.weight_width-1,0,"None","'b1"),
                          ("in","io_ctrl_and_clocks_rx_user_weights_3_2_real",rx.weight_width-1,0,"None","'b1"),
                          ("in","io_ctrl_and_clocks_rx_user_weights_3_2_imag",rx.weight_width-1,0,"None","'b1"),
                          ("in","io_ctrl_and_clocks_rx_user_weights_3_3_real",rx.weight_width-1,0,"None","'b1"),
                          ("in","io_ctrl_and_clocks_rx_user_weights_3_3_imag",rx.weight_width-1,0,"None","'b1"),

                          // neighbour_delays
                          ("in","io_ctrl_and_clocks_neighbour_delays_0_0",rx.delay_width-1,0,"None","'b0"),
                          ("in","io_ctrl_and_clocks_neighbour_delays_0_1",rx.delay_width-1,0,"None","'b0"),
                          ("in","io_ctrl_and_clocks_neighbour_delays_0_2",rx.delay_width-1,0,"None","'b0"),
                          ("in","io_ctrl_and_clocks_neighbour_delays_0_3",rx.delay_width-1,0,"None","'b0"),
                          ("in","io_ctrl_and_clocks_neighbour_delays_1_0",rx.delay_width-1,0,"None","'b0"),
                          ("in","io_ctrl_and_clocks_neighbour_delays_1_1",rx.delay_width-1,0,"None","'b0"),
                          ("in","io_ctrl_and_clocks_neighbour_delays_1_2",rx.delay_width-1,0,"None","'b0"),
                          ("in","io_ctrl_and_clocks_neighbour_delays_1_3",rx.delay_width-1,0,"None","'b0"),
                          ("in","io_ctrl_and_clocks_neighbour_delays_2_0",rx.delay_width-1,0,"None","'b0"),
                          ("in","io_ctrl_and_clocks_neighbour_delays_2_1",rx.delay_width-1,0,"None","'b0"),
                          ("in","io_ctrl_and_clocks_neighbour_delays_2_2",rx.delay_width-1,0,"None","'b0"),
                          ("in","io_ctrl_and_clocks_neighbour_delays_2_3",rx.delay_width-1,0,"None","'b0"),
                          ("in","io_ctrl_and_clocks_neighbour_delays_3_0",rx.delay_width-1,0,"None","'b0"),
                          ("in","io_ctrl_and_clocks_neighbour_delays_3_1",rx.delay_width-1,0,"None","'b0"),
                          ("in","io_ctrl_and_clocks_neighbour_delays_3_2",rx.delay_width-1,0,"None","'b0"),
                          ("in","io_ctrl_and_clocks_neighbour_delays_3_3",rx.delay_width-1,0,"None","'b0"),
                          //Serdes test stuff
                          ("out","io_ctrl_and_clocks_from_serdes_scan_0_ready","None","None","None","None"),
                          ("in","io_ctrl_and_clocks_from_serdes_scan_0_valid","None","None","None","'b1"),
                          ("in","io_ctrl_and_clocks_from_serdes_scan_0_bits_data_0_udata_real",15,0,"None","'b0"),
                          ("in","io_ctrl_and_clocks_from_serdes_scan_0_bits_data_0_udata_imag",15,0,"None","'b0"),
                          ("in","io_ctrl_and_clocks_from_serdes_scan_0_bits_data_0_uindex",1,0,"None","'b0"),
                          ("in","io_ctrl_and_clocks_from_serdes_scan_0_bits_data_1_udata_real",15,0,"None","'b0"),
                          ("in","io_ctrl_and_clocks_from_serdes_scan_0_bits_data_1_udata_imag",15,0,"None","'b0"),
                          ("in","io_ctrl_and_clocks_from_serdes_scan_0_bits_data_1_uindex",1,0,"None","'b0"),
                          ("in","io_ctrl_and_clocks_from_serdes_scan_0_bits_data_2_udata_real",15,0,"None","'b0"),
                          ("in","io_ctrl_and_clocks_from_serdes_scan_0_bits_data_2_udata_imag",15,0,"None","'b0"),
                          ("in","io_ctrl_and_clocks_from_serdes_scan_0_bits_data_2_uindex",1,0,"None","'b0"),
                          ("in","io_ctrl_and_clocks_from_serdes_scan_0_bits_data_3_udata_real",15,0,"None","'b0"),
                          ("in","io_ctrl_and_clocks_from_serdes_scan_0_bits_data_3_udata_imag",15,0,"None","'b0"),
                          ("in","io_ctrl_and_clocks_from_serdes_scan_0_bits_data_3_uindex",1,0,"None","'b0"),
                          ("in","io_ctrl_and_clocks_from_serdes_scan_0_bits_rxindex",1,0,"None","'b0"),
                          ("out","io_ctrl_and_clocks_from_serdes_scan_1_ready","None","None","None","None"),
                          ("in","io_ctrl_and_clocks_from_serdes_scan_1_valid","None","None","None","'b1"),
                          ("in","io_ctrl_and_clocks_from_serdes_scan_1_bits_data_0_udata_real",15,0,"None","'b0"),
                          ("in","io_ctrl_and_clocks_from_serdes_scan_1_bits_data_0_udata_imag",15,0,"None","'b0"),
                          ("in","io_ctrl_and_clocks_from_serdes_scan_1_bits_data_0_uindex",1,0,"None","'b0"),
                          ("in","io_ctrl_and_clocks_from_serdes_scan_1_bits_data_1_udata_real",15,0,"None","'b0"),
                          ("in","io_ctrl_and_clocks_from_serdes_scan_1_bits_data_1_udata_imag",15,0,"None","'b0"),
                          ("in","io_ctrl_and_clocks_from_serdes_scan_1_bits_data_1_uindex",1,0,"None","'b0"),
                          ("in","io_ctrl_and_clocks_from_serdes_scan_1_bits_data_2_udata_real",15,0,"None","'b0"),
                          ("in","io_ctrl_and_clocks_from_serdes_scan_1_bits_data_2_udata_imag",15,0,"None","'b0"),
                          ("in","io_ctrl_and_clocks_from_serdes_scan_1_bits_data_2_uindex",1,0,"None","'b0"),
                          ("in","io_ctrl_and_clocks_from_serdes_scan_1_bits_data_3_udata_real",15,0,"None","'b0"),
                          ("in","io_ctrl_and_clocks_from_serdes_scan_1_bits_data_3_udata_imag",15,0,"None","'b0"),
                          ("in","io_ctrl_and_clocks_from_serdes_scan_1_bits_data_3_uindex",1,0,"None","'b0"),
                          ("in","io_ctrl_and_clocks_from_serdes_scan_1_bits_rxindex",1,0,"None","'b0"),
                          ("out","io_ctrl_and_clocks_from_dsp_scan_0_ready","None","None","None","None"),
                          ("in","io_ctrl_and_clocks_from_dsp_scan_0_valid","None","None","None","'b1"),
                          ("in","io_ctrl_and_clocks_from_dsp_scan_0_bits_data_0_udata_real",15,0,"None","'b0"),
                          ("in","io_ctrl_and_clocks_from_dsp_scan_0_bits_data_0_udata_imag",15,0,"None","'b0"),
                          ("in","io_ctrl_and_clocks_from_dsp_scan_0_bits_data_0_uindex",1,0,"None","'b0"),
                          ("in","io_ctrl_and_clocks_from_dsp_scan_0_bits_data_1_udata_real",15,0,"None","'b0"),
                          ("in","io_ctrl_and_clocks_from_dsp_scan_0_bits_data_1_udata_imag",15,0,"None","'b0"),
                          ("in","io_ctrl_and_clocks_from_dsp_scan_0_bits_data_1_uindex",1,0,"None","'b0"),
                          ("in","io_ctrl_and_clocks_from_dsp_scan_0_bits_data_2_udata_real",15,0,"None","'b0"),
                          ("in","io_ctrl_and_clocks_from_dsp_scan_0_bits_data_2_udata_imag",15,0,"None","'b0"),
                          ("in","io_ctrl_and_clocks_from_dsp_scan_0_bits_data_2_uindex",1,0,"None","'b0"),
                          ("in","io_ctrl_and_clocks_from_dsp_scan_0_bits_data_3_udata_real",15,0,"None","'b0"),
                          ("in","io_ctrl_and_clocks_from_dsp_scan_0_bits_data_3_udata_imag",15,0,"None","'b0"),
                          ("in","io_ctrl_and_clocks_from_dsp_scan_0_bits_data_3_uindex",1,0,"None","'b0"),
                          ("in","io_ctrl_and_clocks_from_dsp_scan_0_bits_rxindex",1,0,"None","'b0"),
                          ("out","io_ctrl_and_clocks_from_dsp_scan_1_ready","None","None","None","None"),
                          ("in","io_ctrl_and_clocks_from_dsp_scan_1_valid","None","None","None","'b1"),
                          ("in","io_ctrl_and_clocks_from_dsp_scan_1_bits_data_0_udata_real",15,0,"None","'b0"),
                          ("in","io_ctrl_and_clocks_from_dsp_scan_1_bits_data_0_udata_imag",15,0,"None","'b0"),
                          ("in","io_ctrl_and_clocks_from_dsp_scan_1_bits_data_0_uindex",1,0,"None","'b0"),
                          ("in","io_ctrl_and_clocks_from_dsp_scan_1_bits_data_1_udata_real",15,0,"None","'b0"),
                          ("in","io_ctrl_and_clocks_from_dsp_scan_1_bits_data_1_udata_imag",15,0,"None","'b0"),
                          ("in","io_ctrl_and_clocks_from_dsp_scan_1_bits_data_1_uindex",1,0,"None","'b0"),
                          ("in","io_ctrl_and_clocks_from_dsp_scan_1_bits_data_2_udata_real",15,0,"None","'b0"),
                          ("in","io_ctrl_and_clocks_from_dsp_scan_1_bits_data_2_udata_imag",15,0,"None","'b0"),
                          ("in","io_ctrl_and_clocks_from_dsp_scan_1_bits_data_2_uindex",1,0,"None","'b0"),
                          ("in","io_ctrl_and_clocks_from_dsp_scan_1_bits_data_3_udata_real",15,0,"None","'b0"),
                          ("in","io_ctrl_and_clocks_from_dsp_scan_1_bits_data_3_udata_imag",15,0,"None","'b0"),
                          ("in","io_ctrl_and_clocks_from_dsp_scan_1_bits_data_3_uindex",1,0,"None","'b0"),
                          ("in","io_ctrl_and_clocks_from_dsp_scan_1_bits_rxindex",1,0,"None","'b0"),
                          ("in","io_ctrl_and_clocks_dsp_to_serdes_address_0",2,0,"None","'b0"),
                          ("in","io_ctrl_and_clocks_dsp_to_serdes_address_1",2,0,"None","'b0"),
                          ("in","io_ctrl_and_clocks_serdes_to_dsp_address_0","None","None","None","'b0"),
                          ("in","io_ctrl_and_clocks_serdes_to_dsp_address_1","None","None","None","'b0"),
                          ("in","io_ctrl_and_clocks_serdes_to_dsp_address_2","None","None","None","'b0"),
                          ("in","io_ctrl_and_clocks_serdes_to_dsp_address_3","None","None","None","'b0"),
                          ("in","io_ctrl_and_clocks_serdes_to_dsp_address_4","None","None","None","'b0"),
                          ("in","io_ctrl_and_clocks_serdes_to_dsp_address_5","None","None","None","'b0"),
                          ("in","io_ctrl_and_clocks_to_serdes_mode_0",1,0,"None","'b1"),
                          ("in","io_ctrl_and_clocks_to_serdes_mode_1",1,0,"None","'b1"),
                          ("in","io_ctrl_and_clocks_to_dsp_mode_0",1,0,"None","'b1"),
                          ("in","io_ctrl_and_clocks_to_dsp_mode_1",1,0,"None","'b1"),
                          ("in","io_ctrl_and_clocks_to_dsp_mode_2",1,0,"None","'b1"),
                          ("in","io_ctrl_and_clocks_to_dsp_mode_3",1,0,"None","'b1"),
                          ("in","io_ctrl_and_clocks_to_dsp_mode_4",1,0,"None","'b1"),
                          ("in","io_ctrl_and_clocks_to_dsp_mode_5",1,0,"None","'b1"),
                          ("in","io_ctrl_and_clocks_serdestest_scan_write_mode",1,0,"None","'b0"),
                          ("in","io_ctrl_and_clocks_serdestest_scan_write_address",12,0,"None","'b0"),
                          ("in","io_ctrl_and_clocks_serdestest_scan_write_value_data_0_udata_real",15,0,"None","'b0"),
                          ("in","io_ctrl_and_clocks_serdestest_scan_write_value_data_0_udata_imag",15,0,"None","'b0"),
                          ("in","io_ctrl_and_clocks_serdestest_scan_write_value_data_0_uindex",1,0,"None","'b0"),
                          ("in","io_ctrl_and_clocks_serdestest_scan_write_value_data_1_udata_real",15,0,"None","'b0"),
                          ("in","io_ctrl_and_clocks_serdestest_scan_write_value_data_1_udata_imag",15,0,"None","'b0"),
                          ("in","io_ctrl_and_clocks_serdestest_scan_write_value_data_1_uindex",1,0,"None","'b0"),
                          ("in","io_ctrl_and_clocks_serdestest_scan_write_value_data_2_udata_real",15,0,"None","'b0"),
                          ("in","io_ctrl_and_clocks_serdestest_scan_write_value_data_2_udata_imag",15,0,"None","'b0"),
                          ("in","io_ctrl_and_clocks_serdestest_scan_write_value_data_2_uindex",1,0,"None","'b0"),
                          ("in","io_ctrl_and_clocks_serdestest_scan_write_value_data_3_udata_real",15,0,"None","'b0"),
                          ("in","io_ctrl_and_clocks_serdestest_scan_write_value_data_3_udata_imag",15,0,"None","'b0"),
                          ("in","io_ctrl_and_clocks_serdestest_scan_write_value_data_3_uindex",1,0,"None","'b0"),
                          ("in","io_ctrl_and_clocks_serdestest_scan_write_value_rxindex",1,0,"None","'b0"),
                          ("in","io_ctrl_and_clocks_serdestest_scan_write_en","None","None","None","'b0"),
                          ("in","io_ctrl_and_clocks_serdestest_scan_read_mode",1,0,"None","'b0"),
                          ("in","io_ctrl_and_clocks_serdestest_scan_read_address",12,0,"None","'b0"),
                          ("out","io_ctrl_and_clocks_serdestest_scan_read_value_data_0_udata_real",15,0,"None","None"),
                          ("out","io_ctrl_and_clocks_serdestest_scan_read_value_data_0_udata_imag",15,0,"None","None"),
                          ("out","io_ctrl_and_clocks_serdestest_scan_read_value_data_0_uindex",1,0,"None","None"),
                          ("out","io_ctrl_and_clocks_serdestest_scan_read_value_data_1_udata_real",15,0,"None","None"),
                          ("out","io_ctrl_and_clocks_serdestest_scan_read_value_data_1_udata_imag",15,0,"None","None"),
                          ("out","io_ctrl_and_clocks_serdestest_scan_read_value_data_1_uindex",1,0,"None","None"),
                          ("out","io_ctrl_and_clocks_serdestest_scan_read_value_data_2_udata_real",15,0,"None","None"),
                          ("out","io_ctrl_and_clocks_serdestest_scan_read_value_data_2_udata_imag",15,0,"None","None"),
                          ("out","io_ctrl_and_clocks_serdestest_scan_read_value_data_2_uindex",1,0,"None","None"),
                          ("out","io_ctrl_and_clocks_serdestest_scan_read_value_data_3_udata_real",15,0,"None","None"),
                          ("out","io_ctrl_and_clocks_serdestest_scan_read_value_data_3_udata_imag",15,0,"None","None"),
                          ("out","io_ctrl_and_clocks_serdestest_scan_read_value_data_3_uindex",1,0,"None","None"),
                          ("out","io_ctrl_and_clocks_serdestest_scan_read_value_rxindex",1,0,"None","None"),
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
                     


        val textTemplate=header+ extpars+"""
                        |//timescale 1ps this should probably be a global model parameter 
                        |parameter integer c_Ts=1/(g_Rs_high*1e-12);
                        |parameter tx_c_ratio=g_Rs_high/(8*g_Rs_low);
                        |parameter rx_c_ratio=g_Rs_high/(8*g_Rs_low);
                        |parameter RESET_TIME = 128*c_Ts; // initially 16
                        |
                        |""".stripMargin('|')+regdef+wiredef+assdef+iofiledef+
                        """|
                        |
                        |integer din0, din1,din2,din3,din4,din5,din6,din7;
                        |integer din8, din9,din10,din11,din12,din13,din14,din15;
                        |integer din16, din17,din18,din19,din20,din21,din22,din23;
                        |integer memaddrcount;
                        |integer initdone, rxdone, txdone;
                        |
                        |//Initializations
                        |initial clock = 1'b0;
                        |initial reset = 1'b0;
                        |
                        |//Clock definitions
                        |always #(c_Ts/2.0) clock = !clock ;
                        | 
                        |//Tx_io
                        |always @(posedge io_ctrl_and_clocks_dac_clocks_0 ) begin 
                        |    //Print only valid values 
                        |    if ((initdone==1) && txdone==0 &&                                                                   
                        |        ~$isunknown(io_Z_0_real_t) &&  ~$isunknown(io_Z_0_real_b) &&
                        |        ~$isunknown(io_Z_1_real_t) &&  ~$isunknown(io_Z_1_real_b) &&
                        |        ~$isunknown(io_Z_2_real_t) &&  ~$isunknown(io_Z_2_real_b) &&
                        |        ~$isunknown(io_Z_3_real_t) &&  ~$isunknown(io_Z_3_real_b) &&
                        |        ~$isunknown(io_Z_0_imag_t) &&  ~$isunknown(io_Z_0_imag_b) &&
                        |        ~$isunknown(io_Z_1_imag_t) &&  ~$isunknown(io_Z_1_imag_b) &&
                        |        ~$isunknown(io_Z_2_imag_t) &&  ~$isunknown(io_Z_2_imag_b) &&
                        |        ~$isunknown(io_Z_3_imag_t) &&  ~$isunknown(io_Z_3_imag_b)
                        |) begin
                        |        $fwrite(f_io_Z, "%b\t%d\t%b\t%d\t%b\t%d\t%b\t%d\t%b\t%d\t%b\t%d\t%b\t%d\t%b\t%d\n", 
                        |                         io_Z_0_real_t, io_Z_0_real_b, 
                        |                         io_Z_0_imag_t, io_Z_0_imag_b, 
                        |                         io_Z_1_real_t, io_Z_1_real_b, 
                        |                         io_Z_1_imag_t, io_Z_1_imag_b, 
                        |                         io_Z_2_real_t, io_Z_2_real_b, 
                        |                         io_Z_2_imag_t, io_Z_2_imag_b, 
                        |                         io_Z_3_real_t, io_Z_3_real_b, 
                        |                         io_Z_3_imag_t, io_Z_3_imag_b); 
                        |    end
                        |    //else begin
                        |         //$display( $time, "Dropping invalid output values at io_Z ");
                        |    //end 
                        |end
                        |//Rx_io
                        |
                        |//Mimic the reading to lanes
                        |always @(posedge lane_clockRef ) begin 
                        |    //Print only valid values 
                        |    if (
                        |        (io_lanes_tx_0_valid==1) &&  (initdone==1) && rxdone==0 &&
                        |        ~$isunknown(io_lanes_tx_0_bits_data_0_udata_real) && ~$isunknown(io_lanes_tx_0_bits_data_0_udata_imag) && ~$isunknown(io_lanes_tx_0_bits_data_0_uindex) &&   
                        |        ~$isunknown(io_lanes_tx_0_bits_data_1_udata_real) && ~$isunknown(io_lanes_tx_0_bits_data_1_udata_imag) && ~$isunknown(io_lanes_tx_0_bits_data_1_uindex) &&   
                        |        ~$isunknown(io_lanes_tx_0_bits_data_2_udata_real) && ~$isunknown(io_lanes_tx_0_bits_data_2_udata_imag) && ~$isunknown(io_lanes_tx_0_bits_data_2_uindex) &&   
                        |        ~$isunknown(io_lanes_tx_0_bits_data_3_udata_real) && ~$isunknown(io_lanes_tx_0_bits_data_3_udata_imag) && ~$isunknown(io_lanes_tx_0_bits_data_3_uindex) &&   
                        |        ~$isunknown(io_lanes_tx_1_bits_data_0_udata_imag) && ~$isunknown(io_lanes_tx_1_bits_data_0_udata_imag) && ~$isunknown(io_lanes_tx_1_bits_data_0_uindex) &&   
                        |        ~$isunknown(io_lanes_tx_1_bits_data_1_udata_imag) && ~$isunknown(io_lanes_tx_1_bits_data_1_udata_imag) && ~$isunknown(io_lanes_tx_1_bits_data_1_uindex) &&   
                        |        ~$isunknown(io_lanes_tx_1_bits_data_2_udata_imag) && ~$isunknown(io_lanes_tx_1_bits_data_2_udata_imag) && ~$isunknown(io_lanes_tx_1_bits_data_2_uindex) &&   
                        |        ~$isunknown(io_lanes_tx_1_bits_data_3_udata_imag) && ~$isunknown(io_lanes_tx_1_bits_data_3_udata_imag) && ~$isunknown(io_lanes_tx_1_bits_data_3_uindex)   
                        |       ) begin
                        |        //$fwrite(f_io_lanes_tx, "%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\n", 
                        |        //$display( $time, "Printing output values at io_lanes_tx");
                        |        $fwrite(f_io_lanes_tx, "%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\n", 
                        |                         io_lanes_tx_0_bits_data_0_udata_real, io_lanes_tx_0_bits_data_0_udata_imag,
                        |                         io_lanes_tx_0_bits_data_1_udata_real, io_lanes_tx_0_bits_data_1_udata_imag,
                        |                         io_lanes_tx_0_bits_data_2_udata_real, io_lanes_tx_0_bits_data_2_udata_imag,
                        |                         io_lanes_tx_0_bits_data_3_udata_real, io_lanes_tx_0_bits_data_3_udata_imag
                        |                         );
                        |                         //io_lanes_tx_1_bits_data_0_udata_real, io_lanes_tx_1_bits_data_0_udata_imag,
                        |                         //io_lanes_tx_1_bits_data_1_udata_real, io_lanes_tx_1_bits_data_1_udata_imag,
                        |                         //io_lanes_tx_1_bits_data_2_udata_real, io_lanes_tx_1_bits_data_2_udata_imag,
                        |                         //io_lanes_tx_1_bits_data_3_udata_real, io_lanes_tx_1_bits_data_3_udata_imag
                        |                         //);
                        |       // $fwrite(f_io_lanes_tx, "%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\n", 
                        |       //                  io_lanes_tx_0_bits_data_0_udata_real, io_lanes_tx_0_bits_data_0_udata_imag, io_lanes_tx_0_bits_data_0_uindex,
                        |       //                  io_lanes_tx_0_bits_data_1_udata_real, io_lanes_tx_0_bits_data_1_udata_imag, io_lanes_tx_0_bits_data_1_uindex,
                        |       //                  io_lanes_tx_0_bits_data_2_udata_real, io_lanes_tx_0_bits_data_2_udata_imag, io_lanes_tx_0_bits_data_2_uindex,
                        |       //                  io_lanes_tx_0_bits_data_3_udata_real, io_lanes_tx_0_bits_data_3_udata_imag, io_lanes_tx_0_bits_data_3_uindex,
                        |       //                  io_lanes_tx_1_bits_data_0_udata_real, io_lanes_tx_1_bits_data_0_udata_imag, io_lanes_tx_1_bits_data_0_uindex,
                        |       //                  io_lanes_tx_1_bits_data_1_udata_real, io_lanes_tx_1_bits_data_1_udata_imag, io_lanes_tx_1_bits_data_1_uindex,
                        |       //                  io_lanes_tx_1_bits_data_2_udata_real, io_lanes_tx_1_bits_data_2_udata_imag, io_lanes_tx_1_bits_data_2_uindex,
                        |       //                  io_lanes_tx_1_bits_data_3_udata_real, io_lanes_tx_1_bits_data_3_udata_imag, io_lanes_tx_1_bits_data_3_uindex);
                        |    end
                        |    //else begin
                        |    //    $display( $time, "Dropping invalid output values at io_lanes_tx");
                        |    //end 
                        |end
                        |
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
                        |        while (!$feof(f_io_lanes_rx)) begin
                        |                txdone<=0;
                        |                //Lane output fifo is red by the symrate clock
                        |                @(posedge io_lanes_tx_deq_clock )
                        |                status_io_lanes_rx=$fscanf(f_io_lanes_rx, "%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\n",
                        |                                din0, din1, din2, din3, din4, din5, din6, din7,
                        |                                din8, din9, din10, din11, din12, din13, din14, din15
                        |                                );
                        |                io_lanes_rx_0_bits_data_0_udata_real <= din0;
                        |                io_lanes_rx_0_bits_data_0_udata_imag <= din1;
                        |                io_lanes_rx_0_bits_data_1_udata_real <= din2;
                        |                io_lanes_rx_0_bits_data_1_udata_imag <= din3;
                        |                io_lanes_rx_0_bits_data_2_udata_real <= din4;
                        |                io_lanes_rx_0_bits_data_2_udata_imag <= din5;
                        |                io_lanes_rx_0_bits_data_3_udata_real <= din6;
                        |                io_lanes_rx_0_bits_data_3_udata_imag <= din7;
                        |                io_lanes_rx_1_bits_data_0_udata_real <=din8;
                        |                io_lanes_rx_1_bits_data_0_udata_imag <=din9;
                        |                io_lanes_rx_1_bits_data_1_udata_real <=din10;
                        |                io_lanes_rx_1_bits_data_1_udata_imag <=din11;
                        |                io_lanes_rx_1_bits_data_2_udata_real <=din12;
                        |                io_lanes_rx_1_bits_data_2_udata_imag <=din13;
                        |                io_lanes_rx_1_bits_data_3_udata_real <=din14;
                        |                io_lanes_rx_1_bits_data_3_udata_imag <=din15;
                        |                txdone<=1;
                        |        end
                        |        while (!$feof(f_io_iptr_A)) begin
                        |                rxdone<=0;
                        |                @(posedge io_ctrl_and_clocks_adc_clocks_0 )
                        |                 status_io_iptr_A=$fscanf(f_io_iptr_A, "%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\n",
                        |                                 din16, din17, din18, din19, din20,din21, din22, din23);
                        |                 io_iptr_A_0_real <= din16;
                        |                 io_iptr_A_0_imag <= din17;
                        |                 io_iptr_A_1_real <= din18;
                        |                 io_iptr_A_1_imag <= din19;
                        |                 io_iptr_A_2_real <= din20;
                        |                 io_iptr_A_2_imag <= din21;
                        |                 io_iptr_A_3_real <= din22;
                        |                 io_iptr_A_3_imag <= din23;
                        |                 rxdone<=1;
                        |        end
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

