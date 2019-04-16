/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mame056.sound;

import static arcadeflex056.fucPtr.*;
import static mame037b11.sound.fmoplH.OPL_TYPE_YM3526;
import static mame056.sndintrfH.*;
import static mame056.sound._3812intf.*;
import static mame056.sound._3812intfH.*;


/**
 *
 * @author jagsanchez
 */
public class _3526intf extends _3812intf {

    public _3526intf() {
        sound_num = SOUND_YM3526;
        name = "YM-3526";
    }

    @Override
    public int chips_num(MachineSound msound) {
        return ((YM3526interface) msound.sound_interface).num;
    }

    @Override
    public int chips_clock(MachineSound msound) {
        return ((YM3526interface) msound.sound_interface).baseclock;
    }

    @Override
    public int start(MachineSound msound) {
        chiptype = OPL_TYPE_YM3526;
        return OPL_sh_start(msound);
    }

    public static ReadHandlerPtr YM3526_status_port_0_r = new ReadHandlerPtr() {
        public int handler(int offset) {
            return YM3812_status_port_0_r.handler(offset);
        }
    };
    public static ReadHandlerPtr YM3526_status_port_1_r = new ReadHandlerPtr() {
        public int handler(int offset) {
            return YM3812_status_port_1_r.handler(offset);
        }
    };
    public static WriteHandlerPtr YM3526_control_port_0_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            YM3812_control_port_0_w.handler(offset, data);
        }
    };
    public static WriteHandlerPtr YM3526_write_port_0_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            YM3812_write_port_0_w.handler(offset, data);
        }
    };
    public static WriteHandlerPtr YM3526_control_port_1_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            YM3812_control_port_1_w.handler(offset, data);
        }
    };
    public static WriteHandlerPtr YM3526_write_port_1_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            YM3812_write_port_1_w.handler(offset, data);
        }
    };
}

