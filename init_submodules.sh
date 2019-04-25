#!/bin/sh
#Init submodules in this dir, if any
DIR="$( cd "$( dirname $0 )" && pwd )"
git submodule update --init

#Dependency satisfied by submodule f2_rx_path
#Publish local the ones you need
#cd $DIR/rocket-chip
#git submodule update --init firrtl
#git submodule update --init chisel3
#git submodule update --init hardfloat
#
#cd $DIR/rocket-chip/firrtl
#sbt publishLocal
#cd $DIR/rocket-chip/chisel3
#sbt publishLocal

#cd $DIR/rocket-chip
#sbt publishLocal

#Prog_delay and clkmux are included in f2_rx_path
SUBMODULES="\
    decouple_branch \
    clkdiv_n_2_4_8 \
    f2_lane_switch \
    f2_serdes_test \
    f2_rx_dsp \
    f2_tx_dsp \
    " 
for module in $SUBMODULES; do
    cd ${DIR}/${module}
    if [ -f "./init_submodules.sh" ]; then
        ./init_submodules.sh
    fi
    sbt publishLocal
done

exit 0

exit 0
