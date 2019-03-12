/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package mame056.machine;

import static arcadeflex056.fucPtr.*;

public class _8255ppiH
{
    /*TODO*///#ifndef _8255PPI_H_
    /*TODO*///#define _8255PPI_H_

    public static int MAX_8255 = 4;
    
    public static class ppi8255_interface
    {
            public int num;							 /* number of PPIs to emulate */
            public ReadHandlerPtr[] portAread;
            public ReadHandlerPtr[] portBread;
            public ReadHandlerPtr[] portCread;
            public WriteHandlerPtr[] portAwrite;
            public WriteHandlerPtr[] portBwrite;
            public WriteHandlerPtr[] portCwrite;

        public ppi8255_interface(int i, ReadHandlerPtr[] ppi_porta_r, ReadHandlerPtr[] ppi_portb_r, ReadHandlerPtr[] ppi_portc_r, WriteHandlerPtr[] ppi_porta_w, WriteHandlerPtr[] ppi_portb_w, WriteHandlerPtr[] ppi_portc_w) {
            num = i;
            
            portAread = ppi_porta_r;
            portBread = ppi_portb_r;
            portCread = ppi_portc_r;
            
            portAwrite = ppi_porta_w;
            portBwrite = ppi_portb_w;
            portCwrite = ppi_portc_w;
        }
        
        public ppi8255_interface(int i, ReadHandlerPtr ppi_porta_r, ReadHandlerPtr ppi_portb_r, ReadHandlerPtr ppi_portc_r, WriteHandlerPtr ppi_porta_w, WriteHandlerPtr ppi_portb_w, WriteHandlerPtr ppi_portc_w) {
            num = i;
            
            portAread = new ReadHandlerPtr[] {ppi_porta_r};
            portBread = new ReadHandlerPtr[] {ppi_portb_r};
            portCread = new ReadHandlerPtr[] {ppi_portc_r};
            
            portAwrite = new WriteHandlerPtr[] {ppi_porta_w};
            portBwrite = new WriteHandlerPtr[] {ppi_portb_w};
            portCwrite = new WriteHandlerPtr[] {ppi_portc_w};
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