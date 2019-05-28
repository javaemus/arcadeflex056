/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */
package mame056.machine;

public class eepromH {
    public static class EEPROM_interface
    {
            public int address_bits;	/* EEPROM has 2^address_bits cells */
            public int data_bits;		/* every cell has this many bits (8 or 16) */
            public String cmd_read;		/*   read command string, e.g. "0110" */
            public String cmd_write;	/*  write command string, e.g. "0111" */
            public String cmd_erase;	/*  erase command string, or 0 if n/a */
            public String cmd_lock;		/*   lock command string, or 0 if n/a */
            public String cmd_unlock;	/* unlock command string, or 0 if n/a */
            public int enable_multi_read;/* set to 1 to enable multiple values to be read from one read command */
            
            public EEPROM_interface(int address_bits, int data_bits, String cmd_read, String cmd_write, String cmd_erase){
                this.address_bits = address_bits;
                this.data_bits = data_bits;
                this.cmd_read = cmd_read;
                this.cmd_write = cmd_write;
                this.cmd_erase = cmd_erase;
            }

        public EEPROM_interface(int address_bits, int data_bits, String cmd_read, String cmd_write, String cmd_erase, String cmd_lock, String cmd_unlock, int enable_multi_read) {
            this.address_bits = address_bits;
            this.data_bits = data_bits;
            this.cmd_read = cmd_read;
            this.cmd_write = cmd_write;
            this.cmd_erase = cmd_erase;
            this.cmd_lock = cmd_lock;
            this.cmd_unlock = cmd_unlock;
            this.enable_multi_read = enable_multi_read;
        }
    };
}
