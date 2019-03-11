/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package mame056.machine;

public class _8255ppiH
{
    /*TODO*///#ifndef _8255PPI_H_
    /*TODO*///#define _8255PPI_H_

    public static int MAX_8255 = 4;
    
    public static abstract interface PortReadHandlerPtr {

        public abstract int handler(int chip);
    }

    public static abstract interface PortWriteHandlerPtr {

        public abstract void handler(int chip, int data);
    }

    public static class ppi8255_interface
    {
            public int num;							 /* number of PPIs to emulate */
            public PortReadHandlerPtr[] portAread;
            public PortReadHandlerPtr[] portBread;
            public PortReadHandlerPtr[] portCread;
            public PortWriteHandlerPtr[] portAwrite;
            public PortWriteHandlerPtr[] portBwrite;
            public PortWriteHandlerPtr[] portCwrite;

        public ppi8255_interface(int i, PortReadHandlerPtr[] ppi_porta_r, PortReadHandlerPtr[] ppi_portb_r, PortReadHandlerPtr[] ppi_portc_r, PortWriteHandlerPtr[] ppi_porta_w, PortWriteHandlerPtr[] ppi_portb_w, PortWriteHandlerPtr[] ppi_portc_w) {
            num = i;
            
            portAread = ppi_porta_r;
            portBread = ppi_portb_r;
            portCread = ppi_portc_r;
            
            portAwrite = ppi_porta_w;
            portBwrite = ppi_portb_w;
            portCwrite = ppi_portc_w;
        }
        
        public ppi8255_interface(int i, PortReadHandlerPtr ppi_porta_r, PortReadHandlerPtr ppi_portb_r, PortReadHandlerPtr ppi_portc_r, PortWriteHandlerPtr ppi_porta_w, PortWriteHandlerPtr ppi_portb_w, PortWriteHandlerPtr ppi_portc_w) {
            num = i;
            
            portAread = new PortReadHandlerPtr[] {ppi_porta_r};
            portBread = new PortReadHandlerPtr[] {ppi_portb_r};
            portCread = new PortReadHandlerPtr[] {ppi_portc_r};
            
            portAwrite = new PortWriteHandlerPtr[] {ppi_porta_w};
            portBwrite = new PortWriteHandlerPtr[] {ppi_portb_w};
            portCwrite = new PortWriteHandlerPtr[] {ppi_portc_w};
        }
    };


    /* Init */
    /*TODO*///void ppi8255_init( ppi8255_interface *intfce);

    /* Read/Write */
    /*TODO*///int ppi8255_r ( int which, int offset );
    /*TODO*///void ppi8255_w( int which, int offset, int data );

    /*TODO*///void ppi8255_set_portAread( int which, mem_read_handler portAread);
    /*TODO*///void ppi8255_set_portBread( int which, mem_read_handler portBread);
    /*TODO*///void ppi8255_set_portCread( int which, mem_read_handler portCread);

    /*TODO*///void ppi8255_set_portAwrite( int which, mem_write_handler portAwrite);
    /*TODO*///void ppi8255_set_portBwrite( int which, mem_write_handler portBwrite);
    /*TODO*///void ppi8255_set_portCwrite( int which, mem_write_handler portCwrite);

    /*TODO*///#ifdef MESS
    /* Peek at the ports */
    /*TODO*///data8_t ppi8255_peek( int which, offs_t offset );
    /*TODO*///#endif

    /* Helpers */
    /*TODO*///#endif
}