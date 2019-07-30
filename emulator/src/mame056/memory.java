/**
 * Ported to 0.56
 */
package mame056;

import arcadeflex056.fucPtr.*;
import static common.ptr.*;
import static common.libc.cstring.*;
import static arcadeflex036.osdepend.*;
import static mame056.cpuintrfH.*;
import static mame056.cpuintrf.*;
import static mame056.cpuexecH.*;
import static mame056.common.*;
import static mame056.commonH.*;
import static mame056.driverH.*;
import static mame056.mame.Machine;
import static mame056.memoryH.*;
import static common.libc.cstdio.*;
import static java.lang.System.exit;

public class memory {

    /**
     * *************************************************************************
     *
     * memory.c
     *
     * Functions which handle the CPU memory and I/O port access.
     *
     * Caveats:
     *
     * The install_mem/port_*_handler functions are only intended to be called
     * at driver init time. Do not call them after this time.
     *
     * If your driver executes an opcode which crosses a bank-switched boundary,
     * it will pull the wrong data out of memory. Although not a common case,
     * you may need to revert to memcpy to work around this. See machine/tnzs.c
     * for an example.
     *
     **************************************************************************
     */
    /**
     * *************************************************************************
     *
     * Basic theory of memory handling:
     *
     * An address with up to 32 bits is passed to a memory handler. First, the
     * non-significant bits are removed from the bottom; for example, a 16-bit
     * memory handler doesn't care about the low bit, so that is removed.
     *
     * Next, the address is broken into two halves, an upper half and a lower
     * half. The number of bits in each half varies based on the total number of
     * address bits. The upper half is then used as an index into the
     * base_lookup table.
     *
     * If the value pulled from the table is within the range 192-255, then the
     * lower half of the address is needed to resolve the final handler. The
     * value from the table (192-255) is combined with the lower address bits to
     * form an index into a subtable.
     *
     * Table values in the range 0-31 are reserved for internal handling (such
     * as RAM, ROM, NOP, and banking). Table values between 32 and 192 are
     * assigned dynamically at startup.
     *
     **************************************************************************
     */

    /*TODO*////* macros for the profiler */
/*TODO*///#define MEMREADSTART			profiler_mark(PROFILER_MEMREAD);
/*TODO*///#define MEMREADEND(ret)			{ profiler_mark(PROFILER_END); return ret; }
/*TODO*///#define MEMWRITESTART			profiler_mark(PROFILER_MEMWRITE);
/*TODO*///#define MEMWRITEEND(ret)		{ (ret); profiler_mark(PROFILER_END); return; }
/*TODO*///
    public static int DATABITS_TO_SHIFT(int d) {
        return (((d) == 32) ? 2 : ((d) == 16) ? 1 : 0);
    }

    /* helper macros */
    public static boolean HANDLER_IS_RAM(int h) {
        return ((h) == STATIC_RAM);
    }

    public static boolean HANDLER_IS_ROM(int h) {
        return ((h) == STATIC_ROM);
    }

    public static boolean HANDLER_IS_RAMROM(int h) {
        return ((h) == STATIC_RAMROM);
    }

    public static boolean HANDLER_IS_NOP(int h) {
        return ((h) == STATIC_NOP);
    }

    public static boolean HANDLER_IS_BANK(int h) {
        return ((h) >= STATIC_BANK1 && (h) <= STATIC_BANKMAX);
    }

    public static boolean HANDLER_IS_STATIC(int h) {
        return (h < STATIC_COUNT && h != -15000);//special handle for arcadeflex
    }

    public static int HANDLER_TO_BANK(int h) {
        return h;
    }

    /*TODO*///#define BANK_TO_HANDLER(b)		((void *)(b))
/*TODO*///

    /*-------------------------------------------------
            TYPE DEFINITIONS
    -------------------------------------------------*/
    public static class bank_data {

        int used;/* is this bank used? */
        int cpunum;/* the CPU it is used for */
        int base;/* the base offset */
        int readoffset;/* original base offset for reads */
        int writeoffset;/* original base offset for writes */
    }

    public static class handler_data {

        public Object handler;/* function pointer for handler */
        public int offset;/* base offset for handler */

        public static handler_data[] create(int n) {
            handler_data[] a = new handler_data[n];
            for (int k = 0; k < n; k++) {
                a[k] = new handler_data();
            }
            return a;
        }
    }

    public static class table_data {

        UBytePtr table;/* pointer to base of table */
        int /*UINT8*/ subtable_count;/* number of subtables used */
        int /*UINT8*/ subtable_alloc;/* number of subtables allocated */
        handler_data[] handlers;/* pointer to which set of handlers */
    }

    public static class memport_data {

        int cpunum;/* CPU index */
        int abits;/* address bits */
        int dbits;/* data bits */
        int ebits;/* effective address bits */
        int /*offs_t*/ mask;/* address mask */
        table_data read = new table_data();/* memory read lookup table */
        table_data write = new table_data();/* memory write lookup table */
    }

    public static class cpu_data {

        UBytePtr rombase;/* ROM base pointer */
        UBytePtr rambase;/* RAM base pointer */
        opbase_handlerPtr opbase;/* opcode base handler */
        memport_data mem = new memport_data();/* memory tables */
        memport_data port = new memport_data();/* port tables */
    }
    /*TODO*///
/*TODO*///struct memory_address_table
/*TODO*///{
/*TODO*///	int 				bits;				/* address bits */
/*TODO*///	read8_handler		handler;			/* handler associated with that */
/*TODO*///};

    /*-------------------------------------------------
	GLOBAL VARIABLES
    -------------------------------------------------*/
    public static UBytePtr OP_ROM = new UBytePtr();/* opcode ROM base */
    public static UBytePtr OP_RAM = new UBytePtr();/* opcode RAM base */
    public static int opcode_entry;/* opcode readmem entry */
    public static UBytePtr readmem_lookup;/* memory read lookup table */
    public static UBytePtr writemem_lookup;/* memory write lookup table */
    public static UBytePtr readport_lookup;/* port read lookup table */
    public static UBytePtr writeport_lookup;/* port write lookup table */
    public static int mem_amask;/* memory address mask */
    public static int port_amask;/* port address mask */

    public static UBytePtr[] cpu_bankbase = new UBytePtr[STATIC_COUNT];/* array of bank bases */
    public static ExtMemory[] ext_memory = ExtMemory.create(MAX_EXT_MEMORY);/* externally-allocated memory */

    public static opbase_handlerPtr opbasefunc;/* opcode base override */

    public static handler_data[] rmemhandler8 = handler_data.create(ENTRY_COUNT);/* 8-bit memory read handlers */
    public static handler_data[] rmemhandler16 = handler_data.create(ENTRY_COUNT);/* 16-bit memory read handlers */
    public static handler_data[] rmemhandler32 = handler_data.create(ENTRY_COUNT);/* 32-bit memory read handlers */
    public static handler_data[] wmemhandler8 = handler_data.create(ENTRY_COUNT);/* 8-bit memory write handlers */
    public static handler_data[] wmemhandler16 = handler_data.create(ENTRY_COUNT);/* 16-bit memory write handlers */
    public static handler_data[] wmemhandler32 = handler_data.create(ENTRY_COUNT);/* 32-bit memory write handlers */

    public static handler_data[] rporthandler8 = handler_data.create(ENTRY_COUNT);/* 8-bit port read handlers */
    public static handler_data[] rporthandler16 = handler_data.create(ENTRY_COUNT);/* 16-bit port read handlers */
    public static handler_data[] rporthandler32 = handler_data.create(ENTRY_COUNT);/* 32-bit port read handlers */
    public static handler_data[] wporthandler8 = handler_data.create(ENTRY_COUNT);/* 8-bit port write handlers */
    public static handler_data[] wporthandler16 = handler_data.create(ENTRY_COUNT);/* 16-bit port write handlers */
    public static handler_data[] wporthandler32 = handler_data.create(ENTRY_COUNT);/* 32-bit port write handlers */

 /*TODO*///static read8_handler 		rmemhandler8s[STATIC_COUNT];	/* copy of 8-bit static read memory handlers */
/*TODO*///static write8_handler 		wmemhandler8s[STATIC_COUNT];	/* copy of 8-bit static write memory handlers */
/*TODO*///
    public static cpu_data[] cpudata = new cpu_data[MAX_CPU];/* data gathered for each CPU */
    public static bank_data[] bankdata = new bank_data[MAX_BANKS];/* data gathered for each bank */
 /*TODO*///
/*TODO*///offs_t encrypted_opcode_start[MAX_CPU],encrypted_opcode_end[MAX_CPU];
/*TODO*///
/*TODO*///

    /*-------------------------------------------------
	memory_init - initialize the memory system
    -------------------------------------------------*/
    public static int memory_init() {
        /* init the static handlers */
        if (init_static() == 0) {
            return 0;
        }

        /* init the CPUs */
        if (init_cpudata() == 0) {
            return 0;
        }

        /* verify the memory handlers and check banks */
        if (verify_memory() == 0) {
            return 0;
        }
        if (verify_ports() == 0) {
            return 0;
        }

        /* allocate memory for sparse address spaces */
        if (allocate_memory() == 0) {
            return 0;
        }

        /* then fill in the tables */
        if (populate_memory() == 0) {
            return 0;
        }
        if (populate_ports() == 0) {
            return 0;
        }
        /*TODO*///	register_banks();
        /* dump the final memory configuration */
        mem_dump();
        return 1;
    }

    /*-------------------------------------------------
	memory_shutdown - free memory
    -------------------------------------------------*/
    public static void memory_shutdown() {
        /*TODO*///	struct ExtMemory *ext;
/*TODO*///	int cpunum;
/*TODO*///
/*TODO*///	/* free all the tables */
/*TODO*///	for (cpunum = 0; cpunum < MAX_CPU; cpunum++ )
/*TODO*///	{
/*TODO*///		if (cpudata[cpunum].mem.read.table)
/*TODO*///			free(cpudata[cpunum].mem.read.table);
/*TODO*///		if (cpudata[cpunum].mem.write.table)
/*TODO*///			free(cpudata[cpunum].mem.write.table);
/*TODO*///		if (cpudata[cpunum].port.read.table)
/*TODO*///			free(cpudata[cpunum].port.read.table);
/*TODO*///		if (cpudata[cpunum].port.write.table)
/*TODO*///			free(cpudata[cpunum].port.write.table);
/*TODO*///	}
/*TODO*///	memset(&cpudata, 0, sizeof(cpudata));
/*TODO*///
/*TODO*///	/* free all the external memory */
/*TODO*///	for (ext = ext_memory; ext->data; ext++)
/*TODO*///		free(ext->data);
/*TODO*///	memset(ext_memory, 0, sizeof(ext_memory));
    }

    /*-------------------------------------------------
	memory_set_opcode_base - set the base of
	ROM
    -------------------------------------------------*/
    public static void memory_set_opcode_base(int cpunum, UBytePtr base) {
        cpudata[cpunum].rombase = base;
    }

    /*TODO*///
/*TODO*///
/*TODO*///void memory_set_encrypted_opcode_range(int cpunum,offs_t min_address,offs_t max_address)
/*TODO*///{
/*TODO*///	encrypted_opcode_start[cpunum] = min_address;
/*TODO*///	encrypted_opcode_end[cpunum] = max_address;
/*TODO*///}
/*TODO*///

    /*-------------------------------------------------
	memory_set_context - set the memory context
    -------------------------------------------------*/
    public static void memory_set_context(int activecpu) {
        OP_RAM = cpu_bankbase[STATIC_RAM] = cpudata[activecpu].rambase;
        OP_ROM = cpudata[activecpu].rombase;
        opcode_entry = STATIC_ROM;

        readmem_lookup = cpudata[activecpu].mem.read.table;
        writemem_lookup = cpudata[activecpu].mem.write.table;
        readport_lookup = cpudata[activecpu].port.read.table;
        writeport_lookup = cpudata[activecpu].port.write.table;

        mem_amask = cpudata[activecpu].mem.mask;
        port_amask = cpudata[activecpu].port.mask;

        opbasefunc = cpudata[activecpu].opbase;
    }

    

    /*-------------------------------------------------
            memory_set_bankhandler_r - set readmemory
            handler for bank memory (8-bit only!)
    -------------------------------------------------*/
    public static void memory_set_bankhandler_r(int bank, int offset, int handler){
        ReadHandlerPtr _handler=new ReadHandlerPtr() {
            public int handler(int offset) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        };
        
        memory_set_bankhandler_r(bank, offset, handler, _handler);
    }
    
    public static void memory_set_bankhandler_r(int bank, int offset, ReadHandlerPtr _handler){
        memory_set_bankhandler_r(bank, offset, -15000, _handler);
    }
    
    public static void memory_set_bankhandler_r(int bank, int offset, int handler, ReadHandlerPtr _handler)
    {
            /* determine the new offset */
            if (HANDLER_IS_RAM(handler) || HANDLER_IS_ROM(handler)){
                    rmemhandler8[bank].offset = 0 - offset;
                    handler = STATIC_RAM;
            } else if (HANDLER_IS_BANK(handler)) {
                    rmemhandler8[bank].offset = bankdata[HANDLER_TO_BANK(handler)].readoffset - offset;
            } else {
                    rmemhandler8[bank].offset = bankdata[bank].readoffset - offset;
            }

            /* set the new handler */
            if (HANDLER_IS_STATIC(handler))
                    _handler = (ReadHandlerPtr) rmemhandler8[handler].handler;
            rmemhandler8[bank].handler = _handler;
    }


    /*-------------------------------------------------
            memory_set_bankhandler_w - set writememory
            handler for bank memory (8-bit only!)
    -------------------------------------------------*/
    public static void memory_set_bankhandler_w(int bank, int offset, WriteHandlerPtr _handler){
        memory_set_bankhandler_w(bank, offset, -15000, _handler);
    }
    
    public static void memory_set_bankhandler_w(int bank, int offset, int handler){
        WriteHandlerPtr _handler = new WriteHandlerPtr() {
            public void handler(int offset, int data) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        };
        memory_set_bankhandler_w(bank, offset, handler, _handler);
    }
            
    public static void memory_set_bankhandler_w(int bank, int offset, int handler, WriteHandlerPtr _handler)
    {
            /* determine the new offset */
            if (HANDLER_IS_RAM(handler) || HANDLER_IS_ROM(handler) || HANDLER_IS_RAMROM(handler))
                    wmemhandler8[bank].offset = 0 - offset;
            else if (HANDLER_IS_BANK(handler))
                    wmemhandler8[bank].offset = bankdata[HANDLER_TO_BANK(handler)].writeoffset - offset;
            else
                    wmemhandler8[bank].offset = bankdata[bank].writeoffset - offset;

            /* set the new handler */
            if (HANDLER_IS_STATIC(handler))
                    _handler = (WriteHandlerPtr) wmemhandler8[handler].handler;
            wmemhandler8[bank].handler = _handler;
    }


    /*-------------------------------------------------
            memory_set_opbase_handler - change op-code
            memory base
    -------------------------------------------------*/
    public static opbase_handlerPtr memory_set_opbase_handler(int cpunum, opbase_handlerPtr function) {
        opbase_handlerPtr old = cpudata[cpunum].opbase;
        cpudata[cpunum].opbase = function;
        if (cpunum == cpu_getactivecpu()) {
            opbasefunc = function;
        }
        return old;
    }


    /*-------------------------------------------------
	install_mem_read_handler - install dynamic
	read handler for 8-bit case
    -------------------------------------------------*/
    public static UBytePtr install_mem_read_handler(int cpunum, int start, int end, ReadHandlerPtr handler) {
        /* sanity check */
        if (cpudata[cpunum].mem.dbits != 8) {
            printf("fatal: install_mem_read_handler called on %d-bit cpu\n", cpudata[cpunum].mem.dbits);
            exit(1);
        }

        /* install the handler */
        install_mem_handler(cpudata[cpunum].mem, 0, start, end, -15000, handler);

        /* dump the new memory configuration */
        mem_dump();

        return memory_find_base(cpunum, start);
    }

    public static UBytePtr install_mem_read_handler(int cpunum, int start, int end, int _handler) {
        /* sanity check */
        if (cpudata[cpunum].mem.dbits != 8) {
            printf("fatal: install_mem_read_handler called on %d-bit cpu\n", cpudata[cpunum].mem.dbits);
            exit(1);
        }

        /* install the handler */
        install_mem_handler(cpudata[cpunum].mem, 0, start, end, _handler, null);

        /* dump the new memory configuration */
        mem_dump();

        return memory_find_base(cpunum, start);
    }


    /*TODO*///
/*TODO*///
/*TODO*////*-------------------------------------------------
/*TODO*///	install_mem_read16_handler - install dynamic
/*TODO*///	read handler for 16-bit case
/*TODO*///-------------------------------------------------*/
/*TODO*///
/*TODO*///data16_t *install_mem_read16_handler(int cpunum, offs_t start, offs_t end, mem_read16_handler handler)
/*TODO*///{
/*TODO*///	/* sanity check */
/*TODO*///	if (cpudata[cpunum].mem.dbits != 16)
/*TODO*///	{
/*TODO*///		printf("fatal: install_mem_read16_handler called on %d-bit cpu\n",cpudata[cpunum].mem.dbits);
/*TODO*///		exit(1);
/*TODO*///	}
/*TODO*///
/*TODO*///	/* install the handler */
/*TODO*///	install_mem_handler(&cpudata[cpunum].mem, 0, start, end, (void *)handler);
/*TODO*///#ifdef MEM_DUMP
/*TODO*///	/* dump the new memory configuration */
/*TODO*///	mem_dump();
/*TODO*///#endif
/*TODO*///	return memory_find_base(cpunum, start);
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*////*-------------------------------------------------
/*TODO*///	install_mem_read32_handler - install dynamic
/*TODO*///	read handler for 32-bit case
/*TODO*///-------------------------------------------------*/
/*TODO*///
/*TODO*///data32_t *install_mem_read32_handler(int cpunum, offs_t start, offs_t end, mem_read32_handler handler)
/*TODO*///{
/*TODO*///	/* sanity check */
/*TODO*///	if (cpudata[cpunum].mem.dbits != 32)
/*TODO*///	{
/*TODO*///		printf("fatal: install_mem_read32_handler called on %d-bit cpu\n",cpudata[cpunum].mem.dbits);
/*TODO*///		exit(1);
/*TODO*///	}
/*TODO*///
/*TODO*///	/* install the handler */
/*TODO*///	install_mem_handler(&cpudata[cpunum].mem, 0, start, end, (void *)handler);
/*TODO*///#ifdef MEM_DUMP
/*TODO*///	/* dump the new memory configuration */
/*TODO*///	mem_dump();
/*TODO*///#endif
/*TODO*///	return memory_find_base(cpunum, start);
/*TODO*///}
    /*-------------------------------------------------
            install_mem_write_handler - install dynamic
            read handler for 8-bit case
    -------------------------------------------------*/
    public static UBytePtr install_mem_write_handler(int cpunum, int start, int end, WriteHandlerPtr handler) {
        /* sanity check */
        if (cpudata[cpunum].mem.dbits != 8) {
            printf("fatal: install_mem_write_handler called on %d-bit cpu\n", cpudata[cpunum].mem.dbits);
            exit(1);
        }

        /* install the handler */
        install_mem_handler(cpudata[cpunum].mem, 1, start, end, -15000, handler);

        /* dump the new memory configuration */
        mem_dump();

        return memory_find_base(cpunum, start);
    }

    public static UBytePtr install_mem_write_handler(int cpunum, int start, int end, int _handler) {
        /* sanity check */
        if (cpudata[cpunum].mem.dbits != 8) {
            printf("fatal: install_mem_write_handler called on %d-bit cpu\n", cpudata[cpunum].mem.dbits);
            exit(1);
        }

        /* install the handler */
        install_mem_handler(cpudata[cpunum].mem, 1, start, end, _handler, null);

        /* dump the new memory configuration */
        mem_dump();

        return memory_find_base(cpunum, start);
    }


    /*TODO*////*-------------------------------------------------
/*TODO*///	install_mem_write16_handler - install dynamic
/*TODO*///	read handler for 16-bit case
/*TODO*///-------------------------------------------------*/
/*TODO*///
/*TODO*///data16_t *install_mem_write16_handler(int cpunum, offs_t start, offs_t end, mem_write16_handler handler)
/*TODO*///{
/*TODO*///	/* sanity check */
/*TODO*///	if (cpudata[cpunum].mem.dbits != 16)
/*TODO*///	{
/*TODO*///		printf("fatal: install_mem_write16_handler called on %d-bit cpu\n",cpudata[cpunum].mem.dbits);
/*TODO*///		exit(1);
/*TODO*///	}
/*TODO*///
/*TODO*///	/* install the handler */
/*TODO*///	install_mem_handler(&cpudata[cpunum].mem, 1, start, end, (void *)handler);
/*TODO*///#ifdef MEM_DUMP
/*TODO*///	/* dump the new memory configuration */
/*TODO*///	mem_dump();
/*TODO*///#endif
/*TODO*///	return memory_find_base(cpunum, start);
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*////*-------------------------------------------------
/*TODO*///	install_mem_write32_handler - install dynamic
/*TODO*///	read handler for 32-bit case
/*TODO*///-------------------------------------------------*/
/*TODO*///
/*TODO*///data32_t *install_mem_write32_handler(int cpunum, offs_t start, offs_t end, mem_write32_handler handler)
/*TODO*///{
/*TODO*///	/* sanity check */
/*TODO*///	if (cpudata[cpunum].mem.dbits != 32)
/*TODO*///	{
/*TODO*///		printf("fatal: install_mem_write32_handler called on %d-bit cpu\n",cpudata[cpunum].mem.dbits);
/*TODO*///		exit(1);
/*TODO*///	}
/*TODO*///
/*TODO*///	/* install the handler */
/*TODO*///	install_mem_handler(&cpudata[cpunum].mem, 1, start, end, (void *)handler);
/*TODO*///#ifdef MEM_DUMP
/*TODO*///	/* dump the new memory configuration */
/*TODO*///	mem_dump();
/*TODO*///#endif
/*TODO*///	return memory_find_base(cpunum, start);
/*TODO*///}
/*TODO*///

    /*-------------------------------------------------
            install_port_read_handler - install dynamic
            read handler for 8-bit case
    -------------------------------------------------*/
    public static void install_port_read_handler(int cpunum, int start, int end, ReadHandlerPtr handler) {
        /* sanity check */
        if (cpudata[cpunum].port.dbits != 8) {
            printf("fatal: install_port_read_handler called on %d-bit cpu\n", cpudata[cpunum].port.dbits);
            exit(1);
        }

        /* install the handler */
        install_port_handler(cpudata[cpunum].port, 0, start, end, -15000, handler);

        /* dump the new memory configuration */
        mem_dump();

    }

    /*TODO*///
/*TODO*///
/*TODO*////*-------------------------------------------------
/*TODO*///	install_port_read16_handler - install dynamic
/*TODO*///	read handler for 16-bit case
/*TODO*///-------------------------------------------------*/
/*TODO*///
/*TODO*///void install_port_read16_handler(int cpunum, offs_t start, offs_t end, port_read16_handler handler)
/*TODO*///{
/*TODO*///	/* sanity check */
/*TODO*///	if (cpudata[cpunum].port.dbits != 16)
/*TODO*///	{
/*TODO*///		printf("fatal: install_port_read16_handler called on %d-bit cpu\n",cpudata[cpunum].port.dbits);
/*TODO*///		exit(1);
/*TODO*///	}
/*TODO*///
/*TODO*///	/* install the handler */
/*TODO*///	install_port_handler(&cpudata[cpunum].port, 0, start, end, (void *)handler);
/*TODO*///#ifdef MEM_DUMP
/*TODO*///	/* dump the new memory configuration */
/*TODO*///	mem_dump();
/*TODO*///#endif
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*////*-------------------------------------------------
/*TODO*///	install_port_read32_handler - install dynamic
/*TODO*///	read handler for 32-bit case
/*TODO*///-------------------------------------------------*/
/*TODO*///
/*TODO*///void install_port_read32_handler(int cpunum, offs_t start, offs_t end, port_read32_handler handler)
/*TODO*///{
/*TODO*///	/* sanity check */
/*TODO*///	if (cpudata[cpunum].port.dbits != 32)
/*TODO*///	{
/*TODO*///		printf("fatal: install_port_read32_handler called on %d-bit cpu\n",cpudata[cpunum].port.dbits);
/*TODO*///		exit(1);
/*TODO*///	}
/*TODO*///
/*TODO*///	/* install the handler */
/*TODO*///	install_port_handler(&cpudata[cpunum].port, 0, start, end, (void *)handler);
/*TODO*///#ifdef MEM_DUMP
/*TODO*///	/* dump the new memory configuration */
/*TODO*///	mem_dump();
/*TODO*///#endif
/*TODO*///}


/*-------------------------------------------------
	install_port_write_handler - install dynamic
	read handler for 8-bit case
-------------------------------------------------*/

    public static void install_port_write_handler(int cpunum, int start, int end, WriteHandlerPtr handler)
    {
            /* sanity check */
            if (cpudata[cpunum].port.dbits != 8)
            {
                    printf("fatal: install_port_write_handler called on %d-bit cpu\n",cpudata[cpunum].port.dbits);
                    exit(1);
            }

            /* install the handler */
            install_port_handler(cpudata[cpunum].port, 1, start, end, -15000, handler);
    /*TODO*///#ifdef MEM_DUMP
    /*TODO*///	/* dump the new memory configuration */
    /*TODO*///	mem_dump();
    /*TODO*///#endif
    }
/*TODO*///
/*TODO*///
/*TODO*////*-------------------------------------------------
/*TODO*///	install_port_write16_handler - install dynamic
/*TODO*///	read handler for 16-bit case
/*TODO*///-------------------------------------------------*/
/*TODO*///
/*TODO*///void install_port_write16_handler(int cpunum, offs_t start, offs_t end, port_write16_handler handler)
/*TODO*///{
/*TODO*///	/* sanity check */
/*TODO*///	if (cpudata[cpunum].port.dbits != 16)
/*TODO*///	{
/*TODO*///		printf("fatal: install_port_write16_handler called on %d-bit cpu\n",cpudata[cpunum].port.dbits);
/*TODO*///		exit(1);
/*TODO*///	}
/*TODO*///
/*TODO*///	/* install the handler */
/*TODO*///	install_port_handler(&cpudata[cpunum].port, 1, start, end, (void *)handler);
/*TODO*///#ifdef MEM_DUMP
/*TODO*///	/* dump the new memory configuration */
/*TODO*///	mem_dump();
/*TODO*///#endif
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*////*-------------------------------------------------
/*TODO*///	install_port_write32_handler - install dynamic
/*TODO*///	read handler for 32-bit case
/*TODO*///-------------------------------------------------*/
/*TODO*///
/*TODO*///void install_port_write32_handler(int cpunum, offs_t start, offs_t end, port_write32_handler handler)
/*TODO*///{
/*TODO*///	/* sanity check */
/*TODO*///	if (cpudata[cpunum].port.dbits != 32)
/*TODO*///	{
/*TODO*///		printf("fatal: install_port_write32_handler called on %d-bit cpu\n",cpudata[cpunum].port.dbits);
/*TODO*///		exit(1);
/*TODO*///	}
/*TODO*///
/*TODO*///	/* install the handler */
/*TODO*///	install_port_handler(&cpudata[cpunum].port, 1, start, end, (void *)handler);
/*TODO*///#ifdef MEM_DUMP
/*TODO*///	/* dump the new memory configuration */
/*TODO*///	mem_dump();
/*TODO*///#endif
/*TODO*///}
/*TODO*///
/*TODO*///
    /*-------------------------------------------------
            fatalerror - display an error message and
            exit immediately
    -------------------------------------------------*/
    public static int fatalerror(String str, Object... arguments) {
        System.out.println(String.format(str, arguments));
        return 0;
    }

    /*-------------------------------------------------
            memory_find_base - return a pointer to the
            base of RAM associated with the given CPU
            and offset
    -------------------------------------------------*/
    public static UBytePtr memory_find_base(int cpunum, int offset) {

        int region = REGION_CPU1 + cpunum;

        /* look in external memory first */
        for (ExtMemory ext : ext_memory) {
            if (ext.data == null) {
                break;
            }
            throw new UnsupportedOperationException("Unsupported");
            /*TODO*///		if (ext->region == region && ext->start <= offset && ext->end >= offset)
/*TODO*///			return (void *)((UINT8 *)ext->data + (offset - ext->start));
        }
        return new UBytePtr(cpudata[cpunum].rambase, offset);
    }

    /*-------------------------------------------------
            get_handler_index - finds the index of a
            handler, or allocates a new one as necessary
    -------------------------------------------------*/
    public static int get_handler_index(handler_data[] table, int handler, Object _handler, int start) {
        int i;

        /* all static handlers are hardcoded */
        if (HANDLER_IS_STATIC(handler)) {
            return handler;
        }

        /* otherwise, we have to search */
        for (i = STATIC_COUNT; i < SUBTABLE_BASE; i++) {
            if (table[i].handler == null) {
                table[i].handler = _handler;
                table[i].offset = start;
            }
            if (table[i].handler == _handler && table[i].offset == start) {
                return i;
            }
        }
        return 0;
    }

    /*-------------------------------------------------
	alloc_new_subtable - allocates more space
	for a new subtable
    -------------------------------------------------*/
    static int/*UINT8*/ alloc_new_subtable(memport_data memport, table_data tabledata, int/*UINT8*/ previous_value) {
        int l1bits = LEVEL1_BITS(memport.ebits);
        int l2bits = LEVEL2_BITS(memport.ebits);

        /* make sure we don't run out */
        if (tabledata.subtable_count + 1 == SUBTABLE_COUNT) {
            fatalerror("error: ran out of memory subtables\n");
        }

        /* allocate more memory if we need to */
        if (tabledata.subtable_count <= tabledata.subtable_alloc) {
            tabledata.subtable_alloc = (tabledata.subtable_alloc + SUBTABLE_ALLOC) & 0xFF;
            //realloc (to be checked)
            UBytePtr temp = new UBytePtr((1 << l1bits) + (tabledata.subtable_alloc << l2bits));//tabledata->table = realloc(tabledata->table, (1 << l1bits) + (tabledata->subtable_alloc << l2bits));
            System.arraycopy(tabledata.table.memory, 0, temp.memory, 0, tabledata.table.memory.length);
            tabledata.table.memory = temp.memory;
            tabledata.table.offset = 0;
        }

        /* initialize the table entries */
        memset(tabledata.table, (1 << l1bits) + (tabledata.subtable_count << l2bits), previous_value & 0xFF, 1 << l2bits);

        /* return the new index */
        return ((SUBTABLE_BASE + (tabledata.subtable_count++)) & 0xFF);
    }


    /*-------------------------------------------------
	populate_table - assign a memory handler to
	a range of addresses
    -------------------------------------------------*/
    public static void populate_table(memport_data memport, int iswrite, int start, int stop, int handler) {
        table_data tabledata = iswrite != 0 ? memport.write : memport.read;
        int minbits = DATABITS_TO_SHIFT(memport.dbits);
        int l1bits = LEVEL1_BITS(memport.ebits);
        int l2bits = LEVEL2_BITS(memport.ebits);
        int l2mask = LEVEL2_MASK(memport.ebits);
        int l1start = start >> (l2bits + minbits);
        int l2start = (start >> minbits) & l2mask;
        int l1stop = stop >> (l2bits + minbits);
        int l2stop = (stop >> minbits) & l2mask;
        int/*UINT8*/ subindex;

        /* sanity check */
        if (start > stop) {
            return;
        }

        /* set the base for non RAM/ROM cases */
        if (handler != STATIC_RAM && handler != STATIC_ROM && handler != STATIC_RAMROM) {
            tabledata.handlers[handler].offset = start;
        }

        /* remember the base for banks */
        if (handler >= STATIC_BANK1 && handler <= STATIC_BANKMAX) {
            if (iswrite != 0) {
                bankdata[handler].writeoffset = start;
            } else {
                bankdata[handler].readoffset = start;
            }
        }
        /* handle the starting edge if it's not on a block boundary */
        if (l2start != 0) {
            /* get the subtable index */
            subindex = tabledata.table.read(l1start);
            if (subindex < SUBTABLE_BASE) {
                tabledata.table.write(l1start, alloc_new_subtable(memport, tabledata, subindex));
                subindex = tabledata.table.read(l1start);
            }
            subindex &= SUBTABLE_MASK;

            /* if the start and stop end within the same block, handle that */
            if (l1start == l1stop) {
                memset(tabledata.table, (1 << l1bits) + (subindex << l2bits) + l2start, handler, l2stop - l2start + 1);
                return;
            }

            /* otherwise, fill until the end */
            memset(tabledata.table, (1 << l1bits) + (subindex << l2bits) + l2start, handler, (1 << l2bits) - l2start);
            if (l1start != Integer.MAX_VALUE) {
                l1start++;
            }
        }

        /* handle the trailing edge if it's not on a block boundary */
        if (l2stop != l2mask) {
            /* get the subtable index */
            subindex = tabledata.table.read(l1stop);
            if (subindex < SUBTABLE_BASE) {
                tabledata.table.write(l1stop, alloc_new_subtable(memport, tabledata, subindex));
                subindex = tabledata.table.read(l1stop);
            }
            subindex &= SUBTABLE_MASK;

            /* fill from the beginning */
            memset(tabledata.table, (1 << l1bits) + (subindex << l2bits), handler, l2stop + 1);

            /* if the start and stop end within the same block, handle that */
            if (l1start == l1stop) {
                return;
            }
            if (l1stop != 0) {
                l1stop--;
            }
        }

        /* now fill in the middle tables */
        if (l1start <= l1stop) {
            memset(tabledata.table, l1start, handler, l1stop - l1start + 1);
        }
    }

    /*TODO*///
/*TODO*///
/*TODO*////*-------------------------------------------------
/*TODO*///	assign_dynamic_bank - finds a free or exact
/*TODO*///	matching bank
/*TODO*///-------------------------------------------------*/
/*TODO*///
/*TODO*///void *assign_dynamic_bank(int cpunum, offs_t start)
/*TODO*///{
/*TODO*///	int bank;
/*TODO*///
/*TODO*///	/* special case: never assign a dynamic bank to an offset that */
/*TODO*///	/* intersects the CPU's region; always use RAM for that */
/*TODO*///	if (start < memory_region_length(REGION_CPU1 + cpunum))
/*TODO*///		return (void *)STATIC_RAM;
/*TODO*///
/*TODO*///	/* loop over banks, searching for an exact match or an empty */
/*TODO*///	for (bank = 1; bank <= MAX_BANKS; bank++)
/*TODO*///		if (!bankdata[bank].used || (bankdata[bank].cpunum == cpunum && bankdata[bank].base == start))
/*TODO*///		{
/*TODO*///			bankdata[bank].used = 1;
/*TODO*///			bankdata[bank].cpunum = cpunum;
/*TODO*///			bankdata[bank].base = start;
/*TODO*///			return BANK_TO_HANDLER(bank);
/*TODO*///		}
/*TODO*///
/*TODO*///	/* if we got here, we failed */
/*TODO*///	fatalerror("cpu #%d: ran out of banks for sparse memory regions!\n", cpunum);
/*TODO*///	return NULL;
/*TODO*///}
/*TODO*///
/*TODO*///
    /*-------------------------------------------------
            install_mem_handler - installs a handler for
            memory operatinos
    -------------------------------------------------*/
    public static void install_mem_handler(memport_data memport, int iswrite, int start, int end, int handler, Object _handler) {
        table_data tabledata = iswrite != 0 ? memport.write : memport.read;
        int /*UINT8*/ idx;

        /* translate ROM and RAMROM to RAM here for read cases */
        if (iswrite == 0) {
            if (HANDLER_IS_ROM(handler) || HANDLER_IS_RAMROM(handler)) {
                handler = MRA_RAM;
            }
        }

        /* assign banks for sparse memory spaces */
        if (IS_SPARSE(memport.abits) && HANDLER_IS_RAM(handler)) {
            throw new UnsupportedOperationException("Unsupported");
            /*TODO*///		handler = (void *)assign_dynamic_bank(memport->cpunum, start);
        }
        /* set the handler */
        idx = get_handler_index(tabledata.handlers, handler, _handler, start);
        populate_table(memport, iswrite, start, end, idx);

        /* if this is a bank, set the bankbase as well */
        if (HANDLER_IS_BANK(handler)) {
            cpu_bankbase[HANDLER_TO_BANK(handler)] = memory_find_base(memport.cpunum, start);
        }
    }

    /*-------------------------------------------------
	install_port_handler - installs a handler for
	port operatinos
    -------------------------------------------------*/
    public static void install_port_handler(memport_data memport, int iswrite, int start, int end, int handler, Object _handler) {
        table_data tabledata = iswrite != 0 ? memport.write : memport.read;
        int /*UINT8*/ idx = get_handler_index(tabledata.handlers, handler, _handler, start);
        populate_table(memport, iswrite, start, end, idx);
    }

    /*-------------------------------------------------
	set_static_handler - handy shortcut for
	setting all 6 handlers for a given index
-------------------------------------------------*/
    static void set_static_handler(int idx,
            ReadHandlerPtr r8handler, /*read16_handler r16handler, read32_handler r32handler,*/
            WriteHandlerPtr w8handler/*, write16_handler w16handler, write32_handler w32handler*/) {

        /*TODO*///	rmemhandler8s[idx] = r8handler;
/*TODO*///	wmemhandler8s[idx] = w8handler;
/*TODO*///
        rmemhandler8[idx].handler = r8handler;
        /*TODO*///	rmemhandler16[idx].handler = (void *)r16handler;
/*TODO*///	rmemhandler32[idx].handler = (void *)r32handler;
        wmemhandler8[idx].handler = w8handler;
        /*TODO*///	wmemhandler16[idx].handler = (void *)w16handler;
/*TODO*///	wmemhandler32[idx].handler = (void *)w32handler;
/*TODO*///
        rporthandler8[idx].handler = r8handler;
        /*TODO*///	rporthandler16[idx].handler = (void *)r16handler;
/*TODO*///	rporthandler32[idx].handler = (void *)r32handler;
        wporthandler8[idx].handler = w8handler;
        /*TODO*///	wporthandler16[idx].handler = (void *)w16handler;
/*TODO*///	wporthandler32[idx].handler = (void *)w32handler;
    }

    /*-------------------------------------------------
	init_cpudata - initialize the cpudata
	structure for each CPU
-------------------------------------------------*/
    static int init_cpudata() {
        int cpunum;

        /* zap the cpudata structure */
        for (int i = 0; i < MAX_CPU; i++) {
            cpudata[i] = new cpu_data();
        }
        /* loop over CPUs */
        for (cpunum = 0; cpunum < cpu_gettotalcpu(); cpunum++) {
            int cputype = Machine.drv.cpu[cpunum].cpu_type & ~CPU_FLAGS_MASK;

            /* set the RAM/ROM base */
            cpudata[cpunum].rambase = cpudata[cpunum].rombase = memory_region(REGION_CPU1 + cpunum);
            cpudata[cpunum].opbase = null;
            /*TODO*///		encrypted_opcode_start[cpunum] = 0;
/*TODO*///		encrypted_opcode_end[cpunum] = 0;

            /* initialize the readmem and writemem tables */
            if (init_memport(cpunum, cpudata[cpunum].mem, mem_address_bits_of_cpu(cputype), cpunum_databus_width(cpunum), 1) == 0) {
                return 0;
            }

            /* initialize the readport and writeport tables */
            if (init_memport(cpunum, cpudata[cpunum].port, port_address_bits_of_cpu(cputype), cpunum_databus_width(cpunum), 0) == 0) {
                return 0;
            }

            /* Z80 port mask kludge */
            if (cputype == CPU_Z80) {
                if ((Machine.drv.cpu[cpunum].cpu_type & CPU_16BIT_PORT) == 0) {
                    cpudata[cpunum].port.mask = 0xff;
                }
            }

        }
        return 1;
    }

    /*-------------------------------------------------
	init_memport - initialize the mem/port data
	structure
    -------------------------------------------------*/
    static int init_memport(int cpunum, memport_data data, int abits, int dbits, int ismemory) {
        /* determine the address and data bits */
        data.cpunum = cpunum;
        data.abits = abits;
        data.dbits = dbits;
        data.ebits = abits - DATABITS_TO_SHIFT(dbits);
        data.mask = 0xffffffff >>> (32 - abits);

        /* allocate memory */
        data.read.table = new UBytePtr(1 << LEVEL1_BITS(data.ebits));
        data.write.table = new UBytePtr(1 << LEVEL1_BITS(data.ebits));

        /* initialize everything to unmapped */
        memset(data.read.table, STATIC_UNMAP, 1 << LEVEL1_BITS(data.ebits));
        memset(data.write.table, STATIC_UNMAP, 1 << LEVEL1_BITS(data.ebits));

        /* initialize the pointers to the handlers */
        if (ismemory != 0) {
            data.read.handlers = (dbits == 32) ? rmemhandler32 : (dbits == 16) ? rmemhandler16 : rmemhandler8;
            data.write.handlers = (dbits == 32) ? wmemhandler32 : (dbits == 16) ? wmemhandler16 : wmemhandler8;
        } else {
            data.read.handlers = (dbits == 32) ? rporthandler32 : (dbits == 16) ? rporthandler16 : rporthandler8;
            data.write.handlers = (dbits == 32) ? wporthandler32 : (dbits == 16) ? wporthandler16 : wporthandler8;
        }
        return 1;
    }

    /*-------------------------------------------------
            verify_memory - verify the memory structs
            and track which banks are referenced
    -------------------------------------------------*/
    public static int verify_memory() {
        int cpunum;

        /* zap the bank data */
        for (int i = 0; i < MAX_BANKS; i++) {
            bankdata[i] = new bank_data();//memset(&bankdata, 0, sizeof(bankdata));
        }

        /* loop over CPUs */
        for (cpunum = 0; cpunum < cpu_gettotalcpu(); cpunum++) {

            int width;
            int bank;

            /* determine the desired width */
            switch (cpunum_databus_width(cpunum)) {
                case 8:
                    width = MEMPORT_WIDTH_8;
                    break;
                case 16:
                    width = MEMPORT_WIDTH_16;
                    break;
                case 32:
                    width = MEMPORT_WIDTH_32;
                    break;
                default:
                    return fatalerror("cpu #%d has invalid memory width!\n", cpunum);
            }
            Object mra_obj = Machine.drv.cpu[cpunum].memory_read;
            Object mwa_obj = Machine.drv.cpu[cpunum].memory_write;

            /* verify the read handlers */
            if (mra_obj != null) {
                if (mra_obj instanceof Memory_ReadAddress[]) {
                    Memory_ReadAddress[] mra = (Memory_ReadAddress[]) mra_obj;
                    int mra_ptr = 0;
                    /* verify the MEMPORT_READ_START header */
                    if (mra[mra_ptr].start == MEMPORT_MARKER && mra[mra_ptr].end != 0) {
                        if ((mra[mra_ptr].end & MEMPORT_TYPE_MASK) != MEMPORT_TYPE_MEM) {
                            return fatalerror("cpu #%d has port handlers in place of memory read handlers!\n", cpunum);
                        }
                        if ((mra[mra_ptr].end & MEMPORT_DIRECTION_MASK) != MEMPORT_DIRECTION_READ) {
                            return fatalerror("cpu #%d has memory write handlers in place of memory read handlers!\n", cpunum);
                        }
                        if ((mra[mra_ptr].end & MEMPORT_WIDTH_MASK) != width) {
                            return fatalerror("cpu #%d uses wrong data width memory handlers! (width = %d, memory = %08x)\n", cpunum, cpunum_databus_width(cpunum), mra[mra_ptr].end);
                        }
                        mra_ptr++;
                    }

                    /* track banks used */
                    for (; !IS_MEMPORT_END(mra[mra_ptr]); mra_ptr++) {
                        if (!IS_MEMPORT_MARKER(mra[mra_ptr]) && HANDLER_IS_BANK(mra[mra_ptr].handler)) {
                            bank = HANDLER_TO_BANK(mra[mra_ptr].handler);
                            bankdata[bank].used = 1;
                            bankdata[bank].cpunum = -1;
                        }
                    }
                } else {
                    //do the same for 16,32bit handlers
                    throw new UnsupportedOperationException("Unsupported");
                }
            }
            /* verify the write handlers */
            if (mwa_obj != null) {
                if (mwa_obj instanceof Memory_WriteAddress[]) {
                    Memory_WriteAddress[] mwa = (Memory_WriteAddress[]) mwa_obj;
                    int mwa_ptr = 0;
                    /* verify the MEMPORT_WRITE_START header */
                    if (mwa[mwa_ptr].start == MEMPORT_MARKER && mwa[mwa_ptr].end != 0) {
                        if ((mwa[mwa_ptr].end & MEMPORT_TYPE_MASK) != MEMPORT_TYPE_MEM) {
                            return fatalerror("cpu #%d has port handlers in place of memory write handlers!\n", cpunum);
                        }
                        if ((mwa[mwa_ptr].end & MEMPORT_DIRECTION_MASK) != MEMPORT_DIRECTION_WRITE) {
                            return fatalerror("cpu #%d has memory read handlers in place of memory write handlers!\n", cpunum);
                        }
                        if ((mwa[mwa_ptr].end & MEMPORT_WIDTH_MASK) != width) {
                            return fatalerror("cpu #%d uses wrong data width memory handlers! (width = %d, memory = %08x)\n", cpunum, cpunum_databus_width(cpunum), mwa[mwa_ptr].end);
                        }
                        mwa_ptr++;
                    }

                    /*TODO*///
/*TODO*///			/* track banks used */
/*TODO*///			for (; !IS_MEMPORT_END(mwa); mwa++)
/*TODO*///				if (!IS_MEMPORT_MARKER(mwa) && HANDLER_IS_BANK(mwa->handler))
/*TODO*///				{
/*TODO*///					bank = HANDLER_TO_BANK(mwa->handler);
/*TODO*///					bankdata[bank].used = 1;
/*TODO*///					bankdata[bank].cpunum = -1;
/*TODO*///				}
/*TODO*///				mwa++;
                } else {
                    //do the same for 16,32bit handlers
                    throw new UnsupportedOperationException("Unsupported");
                }
            }

            /*TODO*///		const struct Memory_ReadAddress *mra = Machine->drv->cpu[cpunum].memory_read;
/*TODO*///		const struct Memory_WriteAddress *mwa = Machine->drv->cpu[cpunum].memory_write;
/*TODO*///
/*TODO*///		/* verify the read handlers */
/*TODO*///		if (mra)
/*TODO*///		{
/*TODO*///			/* verify the MEMPORT_READ_START header */
/*TODO*///			if (mra->start == MEMPORT_MARKER && mra->end != 0)
/*TODO*///			{
/*TODO*///				if ((mra->end & MEMPORT_TYPE_MASK) != MEMPORT_TYPE_MEM)
/*TODO*///					return fatalerror("cpu #%d has port handlers in place of memory read handlers!\n", cpunum);
/*TODO*///				if ((mra->end & MEMPORT_DIRECTION_MASK) != MEMPORT_DIRECTION_READ)
/*TODO*///					return fatalerror("cpu #%d has memory write handlers in place of memory read handlers!\n", cpunum);
/*TODO*///				if ((mra->end & MEMPORT_WIDTH_MASK) != width)
/*TODO*///					return fatalerror("cpu #%d uses wrong data width memory handlers! (width = %d, memory = %08x)\n", cpunum,cpunum_databus_width(cpunum),mra->end);
/*TODO*///				mra++;
/*TODO*///			}
/*TODO*///
/*TODO*///			/* track banks used */
/*TODO*///			for ( ; !IS_MEMPORT_END(mra); mra++)
/*TODO*///				if (!IS_MEMPORT_MARKER(mra) && HANDLER_IS_BANK(mra->handler))
/*TODO*///				{
/*TODO*///					bank = HANDLER_TO_BANK(mra->handler);
/*TODO*///					bankdata[bank].used = 1;
/*TODO*///					bankdata[bank].cpunum = -1;
/*TODO*///				}
/*TODO*///		}
/*TODO*///
/*TODO*///		/* verify the write handlers */
/*TODO*///		if (mwa)
/*TODO*///		{
/*TODO*///			/* verify the MEMPORT_WRITE_START header */
/*TODO*///			if (mwa->start == MEMPORT_MARKER && mwa->end != 0)
/*TODO*///			{
/*TODO*///				if ((mwa->end & MEMPORT_TYPE_MASK) != MEMPORT_TYPE_MEM)
/*TODO*///					return fatalerror("cpu #%d has port handlers in place of memory write handlers!\n", cpunum);
/*TODO*///				if ((mwa->end & MEMPORT_DIRECTION_MASK) != MEMPORT_DIRECTION_WRITE)
/*TODO*///					return fatalerror("cpu #%d has memory read handlers in place of memory write handlers!\n", cpunum);
/*TODO*///				if ((mwa->end & MEMPORT_WIDTH_MASK) != width)
/*TODO*///					return fatalerror("cpu #%d uses wrong data width memory handlers! (width = %d, memory = %08x)\n", cpunum,cpunum_databus_width(cpunum),mwa->end);
/*TODO*///				mwa++;
/*TODO*///			}
/*TODO*///
/*TODO*///			/* track banks used */
/*TODO*///			for (; !IS_MEMPORT_END(mwa); mwa++)
/*TODO*///				if (!IS_MEMPORT_MARKER(mwa) && HANDLER_IS_BANK(mwa->handler))
/*TODO*///				{
/*TODO*///					bank = HANDLER_TO_BANK(mwa->handler);
/*TODO*///					bankdata[bank].used = 1;
/*TODO*///					bankdata[bank].cpunum = -1;
/*TODO*///				}
/*TODO*///				mwa++;
/*TODO*///		}
        }
        return 1;
    }

    /*-------------------------------------------------
    	verify_ports - verify the port structs
    -------------------------------------------------*/
    static int verify_ports() {
        int cpunum;

        /* loop over CPUs */
        for (cpunum = 0; cpunum < cpu_gettotalcpu(); cpunum++) {
            int/*UINT32*/ width;

            /* determine the desired width */
            switch (cpunum_databus_width(cpunum)) {
                case 8:
                    width = MEMPORT_WIDTH_8;
                    break;
                case 16:
                    width = MEMPORT_WIDTH_16;
                    break;
                case 32:
                    width = MEMPORT_WIDTH_32;
                    break;
                default:
                    return fatalerror("cpu #%d has invalid memory width!\n", cpunum);
            }
            Object mra_obj = Machine.drv.cpu[cpunum].port_read;
            Object mwa_obj = Machine.drv.cpu[cpunum].port_write;

            /* verify the read handlers */
            if (mra_obj != null) {
                if (mra_obj instanceof IO_ReadPort[]) {
                    IO_ReadPort[] mra = (IO_ReadPort[]) mra_obj;
                    int mra_ptr = 0;
                    /* verify the PORT_READ_START header */
                    if (mra[mra_ptr].start == MEMPORT_MARKER && mra[mra_ptr].end != 0) {
                        if ((mra[mra_ptr].end & MEMPORT_TYPE_MASK) != MEMPORT_TYPE_IO) {
                            return fatalerror("cpu #%d has memory handlers in place of I/O read handlers!\n", cpunum);
                        }
                        if ((mra[mra_ptr].end & MEMPORT_DIRECTION_MASK) != MEMPORT_DIRECTION_READ) {
                            return fatalerror("cpu #%d has port write handlers in place of port read handlers!\n", cpunum);
                        }
                        if ((mra[mra_ptr].end & MEMPORT_WIDTH_MASK) != width) {
                            return fatalerror("cpu #%d uses wrong data width port handlers! (width = %d, memory = %08x)\n", cpunum, cpunum_databus_width(cpunum), mra[mra_ptr].end);
                        }
                    }
                } else {
                    //do the same for 16,32bit handlers
                    throw new UnsupportedOperationException("Unsupported");
                }
            }

            /* verify the write handlers */
            if (mwa_obj != null) {
                if (mwa_obj instanceof IO_WritePort[]) {
                    IO_WritePort[] mwa = (IO_WritePort[]) mwa_obj;
                    int mwa_ptr = 0;
                    /* verify the PORT_WRITE_START header */
                    if (mwa[mwa_ptr].start == MEMPORT_MARKER && mwa[mwa_ptr].end != 0) {
                        if ((mwa[mwa_ptr].end & MEMPORT_TYPE_MASK) != MEMPORT_TYPE_IO) {
                            return fatalerror("cpu #%d has memory handlers in place of I/O write handlers!\n", cpunum);
                        }
                        if ((mwa[mwa_ptr].end & MEMPORT_DIRECTION_MASK) != MEMPORT_DIRECTION_WRITE) {
                            return fatalerror("cpu #%d has port read handlers in place of port write handlers!\n", cpunum);
                        }
                        if ((mwa[mwa_ptr].end & MEMPORT_WIDTH_MASK) != width) {
                            return fatalerror("cpu #%d uses wrong data width port handlers! (width = %d, memory = %08x)\n", cpunum, cpunum_databus_width(cpunum), mwa[mwa_ptr].end);
                        }
                    }
                } else {
                    //do the same for 16,32bit handlers
                    throw new UnsupportedOperationException("Unsupported");
                }
            }

            /*TODO*///		const struct IO_ReadPort *mra = Machine->drv->cpu[cpunum].port_read;
            /*TODO*///		const struct IO_WritePort *mwa = Machine->drv->cpu[cpunum].port_write;
            /*TODO*///
            /*TODO*///		/* verify the read handlers */
            /*TODO*///		if (mra)
            /*TODO*///		{
            /*TODO*///			/* verify the PORT_READ_START header */
            /*TODO*///			if (mra->start == MEMPORT_MARKER && mra->end != 0)
            /*TODO*///			{
            /*TODO*///				if ((mra->end & MEMPORT_TYPE_MASK) != MEMPORT_TYPE_IO)
            /*TODO*///					return fatalerror("cpu #%d has memory handlers in place of I/O read handlers!\n", cpunum);
            /*TODO*///				if ((mra->end & MEMPORT_DIRECTION_MASK) != MEMPORT_DIRECTION_READ)
            /*TODO*///					return fatalerror("cpu #%d has port write handlers in place of port read handlers!\n", cpunum);
            /*TODO*///				if ((mra->end & MEMPORT_WIDTH_MASK) != width)
            /*TODO*///					return fatalerror("cpu #%d uses wrong data width port handlers! (width = %d, memory = %08x)\n", cpunum,cpunum_databus_width(cpunum),mra->end);
            /*TODO*///			}
            /*TODO*///		}
            /*TODO*///
            /*TODO*///		/* verify the write handlers */
            /*TODO*///		if (mwa)
            /*TODO*///		{
            /*TODO*///			/* verify the PORT_WRITE_START header */
            /*TODO*///			if (mwa->start == MEMPORT_MARKER && mwa->end != 0)
            /*TODO*///			{
            /*TODO*///				if ((mwa->end & MEMPORT_TYPE_MASK) != MEMPORT_TYPE_IO)
            /*TODO*///					return fatalerror("cpu #%d has memory handlers in place of I/O write handlers!\n", cpunum);
            /*TODO*///				if ((mwa->end & MEMPORT_DIRECTION_MASK) != MEMPORT_DIRECTION_WRITE)
            /*TODO*///					return fatalerror("cpu #%d has port read handlers in place of port write handlers!\n", cpunum);
            /*TODO*///				if ((mwa->end & MEMPORT_WIDTH_MASK) != width)
            /*TODO*///					return fatalerror("cpu #%d uses wrong data width port handlers! (width = %d, memory = %08x)\n", cpunum,cpunum_databus_width(cpunum),mwa->end);
            /*TODO*///			}
            /*TODO*///		}
        }
        return 1;
    }

    /*-------------------------------------------------
	needs_ram - returns true if a given type
	of memory needs RAM backing it
    -------------------------------------------------*/
    static boolean needs_ram(int cpunum, int handler, Object _handler) {
        /* RAM, ROM, and banks always need RAM */
        if (HANDLER_IS_RAM(handler) || HANDLER_IS_ROM(handler) || HANDLER_IS_RAMROM(handler) || HANDLER_IS_BANK(handler)) {
            return true;
        } /* NOPs never need RAM */ else if (HANDLER_IS_NOP(handler)) {
            return false;
        } /* otherwise, we only need RAM for sparse memory spaces */ else {
            return IS_SPARSE(cpudata[cpunum].mem.abits);
        }
    }

    /*-------------------------------------------------
    	allocate_memory - allocate memory for
    	sparse CPU address spaces
    -------------------------------------------------*/
    static int allocate_memory() {
        int ext = 0;//struct ExtMemory *ext = ext_memory;
        int cpunum;

        /* don't do it for drivers that don't have ROM (MESS needs this) */
        if (Machine.gamedrv.rom == null) {
            return 1;
        }

        /* loop over all CPUs */
        for (cpunum = 0; cpunum < cpu_gettotalcpu(); cpunum++) {
            int region = REGION_CPU1 + cpunum;
            int region_length = memory_region(region) != null ? memory_region_length(region) : 0;
            int size = region_length;

            /* keep going until we break out */
            while (true) {
                int lowest = Integer.MAX_VALUE, end = 0, lastend;
                Object mra_obj = Machine.drv.cpu[cpunum].memory_read;
                Object mwa_obj = Machine.drv.cpu[cpunum].memory_write;
                if (mra_obj instanceof Memory_ReadAddress[]) {
                    Memory_ReadAddress[] mra = (Memory_ReadAddress[]) mra_obj;
                    int mra_ptr = 0;
                    /* find the base of the lowest memory region that extends past the end */
                    for (mra_ptr = 0; !IS_MEMPORT_END(mra[mra_ptr]); mra_ptr++) {
                        if (!IS_MEMPORT_MARKER(mra[mra_ptr])) {
                            if (mra[mra_ptr].end >= size && mra[mra_ptr].start < lowest && needs_ram(cpunum, mra[mra_ptr].handler, mra[mra_ptr]._handler)) {
                                lowest = mra[mra_ptr].start;
                            }
                        }
                    }
                    Memory_WriteAddress[] mwa = (Memory_WriteAddress[]) mwa_obj;
                    int mwa_ptr = 0;

                    for (mwa_ptr = 0; !IS_MEMPORT_END(mwa[mwa_ptr]); mwa_ptr++) {
                        if (!IS_MEMPORT_MARKER(mwa[mwa_ptr])) {
                            if (mwa[mwa_ptr].end >= size && mwa[mwa_ptr].start < lowest && (mwa[mwa_ptr].base != null || needs_ram(cpunum, mwa[mwa_ptr].handler, mwa[mwa_ptr]._handler))) {
                                lowest = mwa[mwa_ptr].start;
                            }
                        }
                    }

                    /* done if nothing found */
                    if (lowest == Integer.MAX_VALUE) {
                        break;
                    }
                    throw new UnsupportedOperationException("Unimplemented");

                    /*TODO*///			/* now loop until we find the end of this contiguous block of memory */
                    /*TODO*///			lastend = ~0;
                    /*TODO*///			end = lowest;
                    /*TODO*///			while (end != lastend)
                    /*TODO*///			{
                    /*TODO*///				lastend = end;
                    /*TODO*///
                    /*TODO*///				/* find the end of the contiguous block of memory */
                    /*TODO*///				for (mra = Machine->drv->cpu[cpunum].memory_read; !IS_MEMPORT_END(mra); mra++)
                    /*TODO*///					if (!IS_MEMPORT_MARKER(mra))
                    /*TODO*///						if (mra->start <= end+1 && mra->end > end && needs_ram(cpunum, (void *)mra->handler))
                    /*TODO*///							end = mra->end;
                    /*TODO*///
                    /*TODO*///				for (mwa = Machine->drv->cpu[cpunum].memory_write; !IS_MEMPORT_END(mwa); mwa++)
                    /*TODO*///					if (!IS_MEMPORT_MARKER(mwa))
                    /*TODO*///						if (mwa->start <= end+1 && mwa->end > end && (mwa->base || needs_ram(cpunum, (void *)mwa->handler)))
                    /*TODO*///							end = mwa->end;
                    /*TODO*///			}
                }
                /*TODO*///			/* find the base of the lowest memory region that extends past the end */
                /*TODO*///			for (mra = Machine->drv->cpu[cpunum].memory_read; !IS_MEMPORT_END(mra); mra++)
                /*TODO*///				if (!IS_MEMPORT_MARKER(mra))
                /*TODO*///					if (mra->end >= size && mra->start < lowest && needs_ram(cpunum, (void *)mra->handler))
                /*TODO*///						lowest = mra->start;
                /*TODO*///
                /*TODO*///			for (mwa = Machine->drv->cpu[cpunum].memory_write; !IS_MEMPORT_END(mwa); mwa++)
                /*TODO*///				if (!IS_MEMPORT_MARKER(mwa))
                /*TODO*///					if (mwa->end >= size && mwa->start < lowest && (mwa->base || needs_ram(cpunum, (void *)mwa->handler)))
                /*TODO*///						lowest = mwa->start;
                /*TODO*///
                /*TODO*///			/* done if nothing found */
                /*TODO*///			if (lowest == ~0)
                /*TODO*///				break;
                /*TODO*///
                /*TODO*///			/* now loop until we find the end of this contiguous block of memory */
                /*TODO*///			lastend = ~0;
                /*TODO*///			end = lowest;
                /*TODO*///			while (end != lastend)
                /*TODO*///			{
                /*TODO*///				lastend = end;
                /*TODO*///
                /*TODO*///				/* find the end of the contiguous block of memory */
                /*TODO*///				for (mra = Machine->drv->cpu[cpunum].memory_read; !IS_MEMPORT_END(mra); mra++)
                /*TODO*///					if (!IS_MEMPORT_MARKER(mra))
                /*TODO*///						if (mra->start <= end+1 && mra->end > end && needs_ram(cpunum, (void *)mra->handler))
                /*TODO*///							end = mra->end;
                /*TODO*///
                /*TODO*///				for (mwa = Machine->drv->cpu[cpunum].memory_write; !IS_MEMPORT_END(mwa); mwa++)
                /*TODO*///					if (!IS_MEMPORT_MARKER(mwa))
                /*TODO*///						if (mwa->start <= end+1 && mwa->end > end && (mwa->base || needs_ram(cpunum, (void *)mwa->handler)))
                /*TODO*///							end = mwa->end;
                /*TODO*///			}
                /*TODO*///
                /*TODO*///			/* fill in the data structure */
                /*TODO*///			ext->start = lowest;
                /*TODO*///			ext->end = end;
                /*TODO*///			ext->region = region;
                /*TODO*///
                /*TODO*///			/* allocate memory */
                /*TODO*///			ext->data = malloc(end+1 - lowest);
                /*TODO*///			if (!ext->data)
                /*TODO*///				fatalerror("malloc(%d) failed (lowest: %x - end: %x)\n", end + 1 - lowest, lowest, end);
                /*TODO*///
                /*TODO*///			/* reset the memory */
                /*TODO*///			memset(ext->data, 0, end+1 - lowest);
                /*TODO*///
                /*TODO*///			/* prepare for the next loop */
                /*TODO*///			size = ext->end + 1;
                /*TODO*///			ext++;
            }
        }
        return 1;
    }

    /*-------------------------------------------------
    	populate_memory - populate the memory mapping
    	tables with entries
    -------------------------------------------------*/
    public static int populate_memory() {
        int cpunum;

        /* loop over CPUs */
        for (cpunum = 0; cpunum < cpu_gettotalcpu(); cpunum++) {
            Object mra_obj = Machine.drv.cpu[cpunum].memory_read;
            Object mwa_obj = Machine.drv.cpu[cpunum].memory_write;

            /* install the read handlers */
            if (mra_obj != null) {
                if (mra_obj instanceof Memory_ReadAddress[]) {
                    Memory_ReadAddress[] mra = (Memory_ReadAddress[]) mra_obj;
                    int mra_ptr = 0;
                    /* first find the end and check for address bits */
                    for (mra_ptr = 0; !IS_MEMPORT_END(mra[mra_ptr]); mra_ptr++) {
                        if (IS_MEMPORT_MARKER(mra[mra_ptr]) && ((mra[mra_ptr].end & MEMPORT_ABITS_MASK) != 0)) {
                            cpudata[cpunum].mem.mask = 0xffffffff >>> (32 - (mra[mra_ptr].end & MEMPORT_ABITS_VAL_MASK));
                        }
                    }

                    /* then work backwards */
                    for (mra_ptr--; mra_ptr >= 0; mra_ptr--) {
                        if (!IS_MEMPORT_MARKER(mra[mra_ptr])) {
                            install_mem_handler(cpudata[cpunum].mem, 0, mra[mra_ptr].start, mra[mra_ptr].end, mra[mra_ptr].handler, (Object) mra[mra_ptr]._handler);
                        }
                    }
                } else {
                    //16,32bit handling
                    throw new UnsupportedOperationException("Unsupported");
                }
            }

            /* install the write handlers */
            if (mwa_obj != null) {
                if (mwa_obj instanceof Memory_WriteAddress[]) {
                    Memory_WriteAddress[] mwa = (Memory_WriteAddress[]) mwa_obj;
                    int mwa_ptr = 0;
                    /* first find the end and check for address bits */
                    for (mwa_ptr = 0; !IS_MEMPORT_END(mwa[mwa_ptr]); mwa_ptr++) {
                        if (IS_MEMPORT_MARKER(mwa[mwa_ptr]) && (mwa[mwa_ptr].end & MEMPORT_ABITS_MASK) != 0) {
                            cpudata[cpunum].mem.mask = 0xffffffff >>> (32 - (mwa[mwa_ptr].end & MEMPORT_ABITS_VAL_MASK));
                        }
                    }

                    /* then work backwards */
                    for (mwa_ptr--; mwa_ptr >= 0; mwa_ptr--) {
                        if (!IS_MEMPORT_MARKER(mwa[mwa_ptr])) {
                            install_mem_handler(cpudata[cpunum].mem, 1, mwa[mwa_ptr].start, mwa[mwa_ptr].end, mwa[mwa_ptr].handler, mwa[mwa_ptr]._handler);
                            if (mwa[mwa_ptr].base != null) {
                                UBytePtr p = memory_find_base(cpunum, mwa[mwa_ptr].start);
                                mwa[mwa_ptr].base.memory = p.memory;
                                mwa[mwa_ptr].base.offset = p.offset;
                            }
                            if (mwa[mwa_ptr].size != null) {
                                mwa[mwa_ptr].size[0] = mwa[mwa_ptr].end - mwa[mwa_ptr].start + 1;
                            }
                        }
                    }
                } else {
                    //16,32bit handling
                    throw new UnsupportedOperationException("Unsupported");
                }
            }

            /*TODO*///		const struct Memory_ReadAddress *mra, *mra_start = Machine->drv->cpu[cpunum].memory_read;
/*TODO*///		const struct Memory_WriteAddress *mwa, *mwa_start = Machine->drv->cpu[cpunum].memory_write;
/*TODO*///
/*TODO*///		/* install the read handlers */
/*TODO*///		if (mra_start)
/*TODO*///		{
/*TODO*///			/* first find the end and check for address bits */
/*TODO*///			for (mra = mra_start; !IS_MEMPORT_END(mra); mra++)
/*TODO*///				if (IS_MEMPORT_MARKER(mra) && (mra->end & MEMPORT_ABITS_MASK))
/*TODO*///					cpudata[cpunum].mem.mask = 0xffffffffUL >> (32 - (mra->end & MEMPORT_ABITS_VAL_MASK));
/*TODO*///
/*TODO*///			/* then work backwards */
/*TODO*///			for (mra--; mra >= mra_start; mra--)
/*TODO*///				if (!IS_MEMPORT_MARKER(mra))
/*TODO*///					install_mem_handler(&cpudata[cpunum].mem, 0, mra->start, mra->end, (void *)mra->handler);
/*TODO*///		}
/*TODO*///
/*TODO*///		/* install the write handlers */
/*TODO*///		if (mwa_start)
/*TODO*///		{
/*TODO*///			/* first find the end and check for address bits */
/*TODO*///			for (mwa = mwa_start; !IS_MEMPORT_END(mwa); mwa++)
/*TODO*///				if (IS_MEMPORT_MARKER(mwa) && (mwa->end & MEMPORT_ABITS_MASK))
/*TODO*///					cpudata[cpunum].mem.mask = 0xffffffffUL >> (32 - (mwa->end & MEMPORT_ABITS_VAL_MASK));
/*TODO*///
/*TODO*///			/* then work backwards */
/*TODO*///			for (mwa--; mwa >= mwa_start; mwa--)
/*TODO*///				if (!IS_MEMPORT_MARKER(mwa))
/*TODO*///				{
/*TODO*///					install_mem_handler(&cpudata[cpunum].mem, 1, mwa->start, mwa->end, (void *)mwa->handler);
/*TODO*///					if (mwa->base) *mwa->base = memory_find_base(cpunum, mwa->start);
/*TODO*///					if (mwa->size) *mwa->size = mwa->end - mwa->start + 1;
/*TODO*///				}
/*TODO*///		}
        }
        return 1;
    }

    /*-------------------------------------------------
    	populate_ports - populate the port mapping
    	tables with entries
    -------------------------------------------------*/
    static int populate_ports() {
        int cpunum;

        /* loop over CPUs */
        for (cpunum = 0; cpunum < cpu_gettotalcpu(); cpunum++) {
            Object mra_obj = Machine.drv.cpu[cpunum].port_read;
            Object mwa_obj = Machine.drv.cpu[cpunum].port_write;


            /* install the read handlers */
            if (mra_obj != null) {
                if (mra_obj instanceof IO_ReadPort[]) {
                    IO_ReadPort[] mra = (IO_ReadPort[]) mra_obj;
                    int mra_ptr = 0;
                    /* first find the end and check for address bits */
                    for (mra_ptr = 0; !IS_MEMPORT_END(mra[mra_ptr]); mra_ptr++) {
                        if (IS_MEMPORT_MARKER(mra[mra_ptr]) && (mra[mra_ptr].end & MEMPORT_ABITS_MASK) != 0) {
                            cpudata[cpunum].port.mask = 0xffffffff >>> (32 - (mra[mra_ptr].end & MEMPORT_ABITS_VAL_MASK));
                        }
                    }

                    /* then work backwards */
                    for (mra_ptr--; mra_ptr >= 0; mra_ptr--) {
                        if (!IS_MEMPORT_MARKER(mra[mra_ptr])) {
                            install_port_handler(cpudata[cpunum].port, 0, mra[mra_ptr].start, mra[mra_ptr].end, mra[mra_ptr].handler, mra[mra_ptr]._handler);
                        }
                    }
                } else {
                    //16bit -32 bit support
                    throw new UnsupportedOperationException("Unsupported");
                }
            }

            /* install the write handlers */
            if (mwa_obj != null) {
                if (mwa_obj instanceof IO_WritePort[]) {
                    IO_WritePort[] mwa = (IO_WritePort[]) mwa_obj;
                    int mwa_ptr = 0;
                    /* first find the end and check for address bits */
                    for (mwa_ptr = 0; !IS_MEMPORT_END(mwa[mwa_ptr]); mwa_ptr++) {
                        if (IS_MEMPORT_MARKER(mwa[mwa_ptr]) && (mwa[mwa_ptr].end & MEMPORT_ABITS_MASK) != 0) {
                            cpudata[cpunum].port.mask = 0xffffffff >>> (32 - (mwa[mwa_ptr].end & MEMPORT_ABITS_VAL_MASK));
                        }
                    }

                    /* then work backwards */
                    for (mwa_ptr--; mwa_ptr >= 0; mwa_ptr--) {
                        if (!IS_MEMPORT_MARKER(mwa[mwa_ptr])) {
                            install_port_handler(cpudata[cpunum].port, 1, mwa[mwa_ptr].start, mwa[mwa_ptr].end, mwa[mwa_ptr].handler, mwa[mwa_ptr]._handler);
                        }
                    }
                } else {
                    //16bit -32 bit support
                    throw new UnsupportedOperationException("Unsupported");
                }
            }
            /*TODO*///
            /*TODO*///		/* install the read handlers */
            /*TODO*///		if (mra_start)
            /*TODO*///		{
            /*TODO*///			/* first find the end and check for address bits */
            /*TODO*///			for (mra = mra_start; !IS_MEMPORT_END(mra); mra++)
            /*TODO*///				if (IS_MEMPORT_MARKER(mra) && (mra->end & MEMPORT_ABITS_MASK))
            /*TODO*///					cpudata[cpunum].port.mask = 0xffffffffUL >> (32 - (mra->end & MEMPORT_ABITS_VAL_MASK));
            /*TODO*///
            /*TODO*///			/* then work backwards */
            /*TODO*///			for (mra--; mra != mra_start; mra--)
            /*TODO*///				if (!IS_MEMPORT_MARKER(mra))
            /*TODO*///					install_port_handler(&cpudata[cpunum].port, 0, mra->start, mra->end, (void *)mra->handler);
            /*TODO*///		}
            /*TODO*///
            /*TODO*///		/* install the write handlers */
            /*TODO*///		if (mwa_start)
            /*TODO*///		{
            /*TODO*///			/* first find the end and check for address bits */
            /*TODO*///			for (mwa = mwa_start; !IS_MEMPORT_END(mwa); mwa++)
            /*TODO*///				if (IS_MEMPORT_MARKER(mwa) && (mwa->end & MEMPORT_ABITS_MASK))
            /*TODO*///					cpudata[cpunum].port.mask = 0xffffffffUL >> (32 - (mwa->end & MEMPORT_ABITS_VAL_MASK));
            /*TODO*///
            /*TODO*///			/* then work backwards */
            /*TODO*///			for (mwa--; mwa != mwa_start; mwa--)
            /*TODO*///				if (!IS_MEMPORT_MARKER(mwa))
            /*TODO*///					install_port_handler(&cpudata[cpunum].port, 1, mwa->start, mwa->end, (void *)mwa->handler);
            /*TODO*///		}
        }
        return 1;
    }

    /*TODO*///
    /*TODO*///
    /*TODO*////*-------------------------------------------------
    /*TODO*///	register_banks - Registers all memory banks
    /*TODO*///    into the state save system
    /*TODO*///-------------------------------------------------*/
    /*TODO*///typedef struct rg_map_entry {
    /*TODO*///	struct rg_map_entry *next;
    /*TODO*///	UINT32 start;
    /*TODO*///	UINT32 end;
    /*TODO*///	int flags;
    /*TODO*///} rg_map_entry;
    /*TODO*///
    /*TODO*///static rg_map_entry *rg_map = 0;
    /*TODO*///
    /*TODO*///enum {
    /*TODO*///	RG_SAVE_READ  = 0x0001,
    /*TODO*///	RG_DROP_READ  = 0x0002,
    /*TODO*///	RG_READ_MASK  = 0x00ff,
    /*TODO*///
    /*TODO*///	RG_SAVE_WRITE = 0x0100,
    /*TODO*///	RG_DROP_WRITE = 0x0200,
    /*TODO*///	RG_WRITE_MASK = 0xff00
    /*TODO*///};
    /*TODO*///
    /*TODO*///static void rg_add_entry(UINT32 start, UINT32 end, int mode)
    /*TODO*///{
    /*TODO*///	rg_map_entry **cur;
    /*TODO*///	cur = &rg_map;
    /*TODO*///	while(*cur && ((*cur)->end < start))
    /*TODO*///		cur = &(*cur)->next;
    /*TODO*///
    /*TODO*///	while(start <= end)
    /*TODO*///	{
    /*TODO*///		int mask;
    /*TODO*///		if(!*cur || ((*cur)->start > start))
    /*TODO*///		{
    /*TODO*///			rg_map_entry *e = malloc(sizeof(rg_map_entry));
    /*TODO*///			e->start = start;
    /*TODO*///			e->end = *cur && (*cur)->start <= end ? (*cur)->start - 1 : end;
    /*TODO*///			e->flags = mode;
    /*TODO*///			e->next = *cur;
    /*TODO*///			*cur = e;
    /*TODO*///			cur = &(*cur)->next;
    /*TODO*///			start = e->end + 1;
    /*TODO*///			if(start > end)
    /*TODO*///				return;
    /*TODO*///		}
    /*TODO*///
    /*TODO*///		if((*cur)->start < start)
    /*TODO*///		{
    /*TODO*///			rg_map_entry *e = malloc(sizeof(rg_map_entry));
    /*TODO*///			e->start = (*cur)->start;
    /*TODO*///			e->end = start - 1;
    /*TODO*///			e->flags = (*cur)->flags;
    /*TODO*///			e->next = *cur;
    /*TODO*///			(*cur)->start = start;
    /*TODO*///			*cur = e;
    /*TODO*///			cur = &(*cur)->next;
    /*TODO*///		}
    /*TODO*///
    /*TODO*///		if((*cur)->end > end)
    /*TODO*///		{
    /*TODO*///			rg_map_entry *e = malloc(sizeof(rg_map_entry));
    /*TODO*///			e->start = start;
    /*TODO*///			e->end = end;
    /*TODO*///			e->flags = (*cur)->flags;
    /*TODO*///			e->next = *cur;
    /*TODO*///			(*cur)->start = end+1;
    /*TODO*///			*cur = e;
    /*TODO*///		}
    /*TODO*///
    /*TODO*///		mask = 0;
    /*TODO*///
    /*TODO*///		if (mode & RG_READ_MASK)
    /*TODO*///			mask |= RG_READ_MASK;
    /*TODO*///		if (mode & RG_WRITE_MASK)
    /*TODO*///			mask |= RG_WRITE_MASK;
    /*TODO*///
    /*TODO*///		(*cur)->flags = ((*cur)->flags & ~mask) | mode;
    /*TODO*///		start = (*cur)->end + 1;
    /*TODO*///		cur = &(*cur)->next;
    /*TODO*///	}
    /*TODO*///}
    /*TODO*///
    /*TODO*///static void rg_map_clear(void)
    /*TODO*///{
    /*TODO*///	rg_map_entry *e = rg_map;
    /*TODO*///	while(e)
    /*TODO*///	{
    /*TODO*///		rg_map_entry *n = e->next;
    /*TODO*///		free(e);
    /*TODO*///		e = n;
    /*TODO*///	}
    /*TODO*///	rg_map = 0;
    /*TODO*///}
    /*TODO*///
    /*TODO*///static void register_zone(int cpunum, UINT32 start, UINT32 end)
    /*TODO*///{
    /*TODO*///	char name[256];
    /*TODO*///	sprintf (name, "%08x-%08x", start, end);
    /*TODO*///	switch (cpunum_databus_width(cpunum))
    /*TODO*///	{
    /*TODO*///	case 8:
    /*TODO*///		state_save_register_UINT8 ("memory", cpunum, name, memory_find_base(cpunum, start), end-start+1);
    /*TODO*///		break;
    /*TODO*///	case 16:
    /*TODO*///		state_save_register_UINT16("memory", cpunum, name, memory_find_base(cpunum, start), (end-start+1)/2);
    /*TODO*///		break;
    /*TODO*///	case 32:
    /*TODO*///		state_save_register_UINT32("memory", cpunum, name, memory_find_base(cpunum, start), (end-start+1)/4);
    /*TODO*///		break;
    /*TODO*///	}
    /*TODO*///}
    /*TODO*///
    /*TODO*///void register_banks(void)
    /*TODO*///{
    /*TODO*///	int cpunum, i;
    /*TODO*///	int banksize[MAX_BANKS];
    /*TODO*///	int bankcpu[MAX_BANKS];
    /*TODO*///
    /*TODO*///	for (i=0; i<MAX_BANKS; i++)
    /*TODO*///	{
    /*TODO*///		banksize[i] = 0;
    /*TODO*///		bankcpu[i] = -1;
    /*TODO*///	}
    /*TODO*///
    /*TODO*///	/* loop over CPUs */
    /*TODO*///	for (cpunum = 0; cpunum < cpu_gettotalcpu(); cpunum++)
    /*TODO*///	{
    /*TODO*///		const struct Memory_ReadAddress *mra, *mra_start = Machine->drv->cpu[cpunum].memory_read;
    /*TODO*///		const struct Memory_WriteAddress *mwa, *mwa_start = Machine->drv->cpu[cpunum].memory_write;
    /*TODO*///		int bits = cpudata[cpunum].mem.abits;
    /*TODO*/////		int width = cpunum_databus_width(cpunum);
    /*TODO*///
    /*TODO*///		if (!IS_SPARSE(bits))
    /*TODO*///		{
    /*TODO*///			UINT32 size = memory_region_length(REGION_CPU1 + cpunum);
    /*TODO*///			if (size > (1<<bits))
    /*TODO*///				size = 1 << bits;
    /*TODO*///			rg_add_entry(0, size-1, RG_SAVE_READ|RG_SAVE_WRITE);
    /*TODO*///		}
    /*TODO*///
    /*TODO*///
    /*TODO*///		if (mra_start)
    /*TODO*///		{
    /*TODO*///			for (mra = mra_start; !IS_MEMPORT_END(mra); mra++);
    /*TODO*///			mra--;
    /*TODO*///			for (;mra != mra_start; mra--)
    /*TODO*///			{
    /*TODO*///				if (!IS_MEMPORT_MARKER (mra))
    /*TODO*///				{
    /*TODO*///					int mode;
    /*TODO*///					mem_read_handler h = mra->handler;
    /*TODO*///					if (!HANDLER_IS_STATIC (h))
    /*TODO*///						mode = RG_DROP_READ;
    /*TODO*///					else if (HANDLER_IS_RAM(h))
    /*TODO*///						mode = RG_SAVE_READ;
    /*TODO*///					else if (HANDLER_IS_ROM(h))
    /*TODO*///						mode = RG_DROP_READ;
    /*TODO*///					else if (HANDLER_IS_RAMROM(h))
    /*TODO*///						mode = RG_SAVE_READ;
    /*TODO*///					else if (HANDLER_IS_NOP(h))
    /*TODO*///						mode = RG_DROP_READ;
    /*TODO*///					else if (HANDLER_IS_BANK(h))
    /*TODO*///					{
    /*TODO*///						int size = mra->end-mra->start+1;
    /*TODO*///						if (banksize[HANDLER_TO_BANK(h)] < size)
    /*TODO*///							banksize[HANDLER_TO_BANK(h)] = size;
    /*TODO*///						bankcpu[HANDLER_TO_BANK(h)] = cpunum;
    /*TODO*///						mode = RG_DROP_READ;
    /*TODO*///					}
    /*TODO*///					else
    /*TODO*///						abort();
    /*TODO*///					rg_add_entry(mra->start, mra->end, mode);
    /*TODO*///				}
    /*TODO*///			}
    /*TODO*///		}
    /*TODO*///		if (mwa_start)
    /*TODO*///		{
    /*TODO*///			for (mwa = mwa_start; !IS_MEMPORT_END(mwa); mwa++);
    /*TODO*///			mwa--;
    /*TODO*///			for (;mwa != mwa_start; mwa--)
    /*TODO*///			{
    /*TODO*///				if (!IS_MEMPORT_MARKER (mwa))
    /*TODO*///				{
    /*TODO*///					int mode;
    /*TODO*///					mem_write_handler h = mwa->handler;
    /*TODO*///					if (!HANDLER_IS_STATIC (h))
    /*TODO*///						mode = mwa->base ? RG_SAVE_WRITE : RG_DROP_WRITE;
    /*TODO*///					else if (HANDLER_IS_RAM(h))
    /*TODO*///						mode = RG_SAVE_WRITE;
    /*TODO*///					else if (HANDLER_IS_ROM(h))
    /*TODO*///						mode = RG_DROP_WRITE;
    /*TODO*///					else if (HANDLER_IS_RAMROM(h))
    /*TODO*///						mode = RG_SAVE_WRITE;
    /*TODO*///					else if (HANDLER_IS_NOP(h))
    /*TODO*///						mode = RG_DROP_WRITE;
    /*TODO*///					else if (HANDLER_IS_BANK(h))
    /*TODO*///					{
    /*TODO*///						int size = mwa->end-mwa->start+1;
    /*TODO*///						if (banksize[HANDLER_TO_BANK(h)] < size)
    /*TODO*///							banksize[HANDLER_TO_BANK(h)] = size;
    /*TODO*///						bankcpu[HANDLER_TO_BANK(h)] = cpunum;
    /*TODO*///						mode = RG_DROP_WRITE;;
    /*TODO*///					}
    /*TODO*///					else
    /*TODO*///						abort();
    /*TODO*///					rg_add_entry(mwa->start, mwa->end, mode);
    /*TODO*///				}
    /*TODO*///			}
    /*TODO*///		}
    /*TODO*///
    /*TODO*///		{
    /*TODO*///			rg_map_entry *e = rg_map;
    /*TODO*///			UINT32 start = 0, end = 0;
    /*TODO*///			int active = 0;
    /*TODO*///			while (e)
    /*TODO*///			{
    /*TODO*///				if(e && (e->flags & (RG_SAVE_READ|RG_SAVE_WRITE)))
    /*TODO*///				{
    /*TODO*///					if (!active)
    /*TODO*///					{
    /*TODO*///						active = 1;
    /*TODO*///						start = e->start;
    /*TODO*///					}
    /*TODO*///					end = e->end;
    /*TODO*///				}
    /*TODO*///				else if (active)
    /*TODO*///				{
    /*TODO*///					register_zone (cpunum, start, end);
    /*TODO*///					active = 0;
    /*TODO*///				}
    /*TODO*///
    /*TODO*///				if (active && (!e->next || (e->end+1 != e->next->start)))
    /*TODO*///				{
    /*TODO*///					register_zone (cpunum, start, end);
    /*TODO*///					active = 0;
    /*TODO*///				}
    /*TODO*///				e = e->next;
    /*TODO*///			}
    /*TODO*///		}
    /*TODO*///
    /*TODO*///		rg_map_clear();
    /*TODO*///	}
    /*TODO*///
    /*TODO*///	for (i=0; i<MAX_BANKS; i++)
    /*TODO*///		if (banksize[i])
    /*TODO*///			switch (cpunum_databus_width(bankcpu[i]))
    /*TODO*///			{
    /*TODO*///			case 8:
    /*TODO*///				state_save_register_UINT8 ("bank", i, "ram",           cpu_bankbase[i], banksize[i]);
    /*TODO*///				break;
    /*TODO*///			case 16:
    /*TODO*///				state_save_register_UINT16("bank", i, "ram", (UINT16 *)cpu_bankbase[i], banksize[i]/2);
    /*TODO*///				break;
    /*TODO*///			case 32:
    /*TODO*///				state_save_register_UINT32("bank", i, "ram", (UINT32 *)cpu_bankbase[i], banksize[i]/4);
    /*TODO*///				break;
    /*TODO*///			}
    /*TODO*///
    /*TODO*///}
    /*TODO*///
    /*TODO*////*-------------------------------------------------
    /*TODO*///	READBYTE - generic byte-sized read handler
    /*TODO*///-------------------------------------------------*/
    /*TODO*///
    /*TODO*///#define READBYTE8(name,abits,lookup,handlist,mask)										\
    /*TODO*///data8_t name(offs_t address)															\
    /*TODO*///{																						\
    /*TODO*///	UINT8 entry;																		\
    /*TODO*///	MEMREADSTART																		\
    /*TODO*///																						\
    /*TODO*///	/* perform lookup */																\
    /*TODO*///	address &= mask;																	\
    /*TODO*///	entry = lookup[LEVEL1_INDEX(address,abits,0)];										\
    /*TODO*///	if (entry >= SUBTABLE_BASE)															\
    /*TODO*///		entry = lookup[LEVEL2_INDEX(entry,address,abits,0)];							\
    /*TODO*///																						\
    /*TODO*///	/* for compatibility with setbankhandler, 8-bit systems */							\
    /*TODO*///	/* must call handlers for banks */													\
    /*TODO*///	if (entry == STATIC_RAM)															\
    /*TODO*///		MEMREADEND(cpu_bankbase[STATIC_RAM][address])									\
    /*TODO*///																						\
    /*TODO*///	/* fall back to the handler */														\
    /*TODO*///	else																				\
    /*TODO*///	{																					\
    /*TODO*///		read8_handler handler = (read8_handler)handlist[entry].handler;					\
    /*TODO*///		MEMREADEND((*handler)(address - handlist[entry].offset))						\
    /*TODO*///	}																					\
    /*TODO*///	return 0;																			\
    /*TODO*///}																						\
    /*TODO*///
    /*TODO*///#define READBYTE16BE(name,abits,lookup,handlist,mask)									\
    /*TODO*///data8_t name(offs_t address)															\
    /*TODO*///{																						\
    /*TODO*///	UINT8 entry;																		\
    /*TODO*///	MEMREADSTART																		\
    /*TODO*///																						\
    /*TODO*///	/* perform lookup */																\
    /*TODO*///	address &= mask;																	\
    /*TODO*///	entry = lookup[LEVEL1_INDEX(address,abits,1)];										\
    /*TODO*///	if (entry >= SUBTABLE_BASE)															\
    /*TODO*///		entry = lookup[LEVEL2_INDEX(entry,address,abits,1)];							\
    /*TODO*///																						\
    /*TODO*///	/* handle banks inline */															\
    /*TODO*///	address -= handlist[entry].offset;													\
    /*TODO*///	if (entry <= STATIC_RAM)															\
    /*TODO*///		MEMREADEND(cpu_bankbase[entry][BYTE_XOR_BE(address)])							\
    /*TODO*///																						\
    /*TODO*///	/* fall back to the handler */														\
    /*TODO*///	else																				\
    /*TODO*///	{																					\
    /*TODO*///		int shift = 8 * (~address & 1);													\
    /*TODO*///		read16_handler handler = (read16_handler)handlist[entry].handler;				\
    /*TODO*///		MEMREADEND((*handler)(address >> 1, ~(0xff << shift)) >> shift)					\
    /*TODO*///	}																					\
    /*TODO*///	return 0;																			\
    /*TODO*///}																						\
    /*TODO*///
    /*TODO*///#define READBYTE16LE(name,abits,lookup,handlist,mask)									\
    /*TODO*///data8_t name(offs_t address)															\
    /*TODO*///{																						\
    /*TODO*///	UINT8 entry;																		\
    /*TODO*///	MEMREADSTART																		\
    /*TODO*///																						\
    /*TODO*///	/* perform lookup */																\
    /*TODO*///	address &= mask;																	\
    /*TODO*///	entry = lookup[LEVEL1_INDEX(address,abits,1)];										\
    /*TODO*///	if (entry >= SUBTABLE_BASE)															\
    /*TODO*///		entry = lookup[LEVEL2_INDEX(entry,address,abits,1)];							\
    /*TODO*///																						\
    /*TODO*///	/* handle banks inline */															\
    /*TODO*///	address -= handlist[entry].offset;													\
    /*TODO*///	if (entry <= STATIC_RAM)															\
    /*TODO*///		MEMREADEND(cpu_bankbase[entry][BYTE_XOR_LE(address)])							\
    /*TODO*///																						\
    /*TODO*///	/* fall back to the handler */														\
    /*TODO*///	else																				\
    /*TODO*///	{																					\
    /*TODO*///		int shift = 8 * (address & 1);													\
    /*TODO*///		read16_handler handler = (read16_handler)handlist[entry].handler;				\
    /*TODO*///		MEMREADEND((*handler)(address >> 1, ~(0xff << shift)) >> shift)					\
    /*TODO*///	}																					\
    /*TODO*///	return 0;																			\
    /*TODO*///}																						\
    /*TODO*///
    /*TODO*///#define READBYTE32BE(name,abits,lookup,handlist,mask)									\
    /*TODO*///data8_t name(offs_t address)															\
    /*TODO*///{																						\
    /*TODO*///	UINT8 entry;																		\
    /*TODO*///	MEMREADSTART																		\
    /*TODO*///																						\
    /*TODO*///	/* perform lookup */																\
    /*TODO*///	address &= mask;																	\
    /*TODO*///	entry = lookup[LEVEL1_INDEX(address,abits,2)];										\
    /*TODO*///	if (entry >= SUBTABLE_BASE)															\
    /*TODO*///		entry = lookup[LEVEL2_INDEX(entry,address,abits,2)];							\
    /*TODO*///																						\
    /*TODO*///	/* handle banks inline */															\
    /*TODO*///	address -= handlist[entry].offset;													\
    /*TODO*///	if (entry <= STATIC_RAM)															\
    /*TODO*///		MEMREADEND(cpu_bankbase[entry][BYTE4_XOR_BE(address)])							\
    /*TODO*///																						\
    /*TODO*///	/* fall back to the handler */														\
    /*TODO*///	else																				\
    /*TODO*///	{																					\
    /*TODO*///		int shift = 8 * (~address & 3);													\
    /*TODO*///		read32_handler handler = (read32_handler)handlist[entry].handler;				\
    /*TODO*///		MEMREADEND((*handler)(address >> 2, ~(0xff << shift)) >> shift) 				\
    /*TODO*///	}																					\
    /*TODO*///	return 0;																			\
    /*TODO*///}																						\
    /*TODO*///
    /*TODO*///#define READBYTE32LE(name,abits,lookup,handlist,mask)									\
    /*TODO*///data8_t name(offs_t address)															\
    /*TODO*///{																						\
    /*TODO*///	UINT8 entry;																		\
    /*TODO*///	MEMREADSTART																		\
    /*TODO*///																						\
    /*TODO*///	/* perform lookup */																\
    /*TODO*///	address &= mask;																	\
    /*TODO*///	entry = lookup[LEVEL1_INDEX(address,abits,2)];										\
    /*TODO*///	if (entry >= SUBTABLE_BASE)															\
    /*TODO*///		entry = lookup[LEVEL2_INDEX(entry,address,abits,2)];							\
    /*TODO*///																						\
    /*TODO*///	/* handle banks inline */															\
    /*TODO*///	address -= handlist[entry].offset;													\
    /*TODO*///	if (entry <= STATIC_RAM)															\
    /*TODO*///		MEMREADEND(cpu_bankbase[entry][BYTE4_XOR_LE(address)])							\
    /*TODO*///																						\
    /*TODO*///	/* fall back to the handler */														\
    /*TODO*///	else																				\
    /*TODO*///	{																					\
    /*TODO*///		int shift = 8 * (address & 3);													\
    /*TODO*///		read32_handler handler = (read32_handler)handlist[entry].handler;				\
    /*TODO*///		MEMREADEND((*handler)(address >> 2, ~(0xff << shift)) >> shift) 				\
    /*TODO*///	}																					\
    /*TODO*///	return 0;																			\
    /*TODO*///}																						\
    /*TODO*///
    /*TODO*///
    /*TODO*////*-------------------------------------------------
    /*TODO*///	READWORD - generic word-sized read handler
    /*TODO*///	(16-bit and 32-bit aligned only!)
    /*TODO*///-------------------------------------------------*/
    /*TODO*///
    /*TODO*///#define READWORD16(name,abits,lookup,handlist,mask)										\
    /*TODO*///data16_t name(offs_t address)															\
    /*TODO*///{																						\
    /*TODO*///	UINT8 entry;																		\
    /*TODO*///	MEMREADSTART																		\
    /*TODO*///																						\
    /*TODO*///	/* perform lookup */																\
    /*TODO*///	address &= mask;																	\
    /*TODO*///	entry = lookup[LEVEL1_INDEX(address,abits,1)];										\
    /*TODO*///	if (entry >= SUBTABLE_BASE)															\
    /*TODO*///		entry = lookup[LEVEL2_INDEX(entry,address,abits,1)];							\
    /*TODO*///																						\
    /*TODO*///	/* handle banks inline */															\
    /*TODO*///	address -= handlist[entry].offset;													\
    /*TODO*///	if (entry <= STATIC_RAM)															\
    /*TODO*///		MEMREADEND(*(data16_t *)&cpu_bankbase[entry][address])							\
    /*TODO*///																						\
    /*TODO*///	/* fall back to the handler */														\
    /*TODO*///	else																				\
    /*TODO*///	{																					\
    /*TODO*///		read16_handler handler = (read16_handler)handlist[entry].handler;				\
    /*TODO*///		MEMREADEND((*handler)(address >> 1,0))										 	\
    /*TODO*///	}																					\
    /*TODO*///	return 0;																			\
    /*TODO*///}																						\
    /*TODO*///
    /*TODO*///#define READWORD32BE(name,abits,lookup,handlist,mask)									\
    /*TODO*///data16_t name(offs_t address)															\
    /*TODO*///{																						\
    /*TODO*///	UINT8 entry;																		\
    /*TODO*///	MEMREADSTART																		\
    /*TODO*///																						\
    /*TODO*///	/* perform lookup */																\
    /*TODO*///	address &= mask;																	\
    /*TODO*///	entry = lookup[LEVEL1_INDEX(address,abits,2)];										\
    /*TODO*///	if (entry >= SUBTABLE_BASE)															\
    /*TODO*///		entry = lookup[LEVEL2_INDEX(entry,address,abits,2)];							\
    /*TODO*///																						\
    /*TODO*///	/* handle banks inline */															\
    /*TODO*///	address -= handlist[entry].offset;													\
    /*TODO*///	if (entry <= STATIC_RAM)															\
    /*TODO*///		MEMREADEND(*(data16_t *)&cpu_bankbase[entry][WORD_XOR_BE(address)])				\
    /*TODO*///																						\
    /*TODO*///	/* fall back to the handler */														\
    /*TODO*///	else																				\
    /*TODO*///	{																					\
    /*TODO*///		int shift = 8 * (~address & 2);													\
    /*TODO*///		read32_handler handler = (read32_handler)handlist[entry].handler;				\
    /*TODO*///		MEMREADEND((*handler)(address >> 2, ~(0xffff << shift)) >> shift)				\
    /*TODO*///	}																					\
    /*TODO*///	return 0;																			\
    /*TODO*///}																						\
    /*TODO*///
    /*TODO*///#define READWORD32LE(name,abits,lookup,handlist,mask)									\
    /*TODO*///data16_t name(offs_t address)															\
    /*TODO*///{																						\
    /*TODO*///	UINT8 entry;																		\
    /*TODO*///	MEMREADSTART																		\
    /*TODO*///																						\
    /*TODO*///	/* perform lookup */																\
    /*TODO*///	address &= mask;																	\
    /*TODO*///	entry = lookup[LEVEL1_INDEX(address,abits,2)];										\
    /*TODO*///	if (entry >= SUBTABLE_BASE)															\
    /*TODO*///		entry = lookup[LEVEL2_INDEX(entry,address,abits,2)];							\
    /*TODO*///																						\
    /*TODO*///	/* handle banks inline */															\
    /*TODO*///	address -= handlist[entry].offset;													\
    /*TODO*///	if (entry <= STATIC_RAM)															\
    /*TODO*///		MEMREADEND(*(data16_t *)&cpu_bankbase[entry][WORD_XOR_LE(address)])				\
    /*TODO*///																						\
    /*TODO*///	/* fall back to the handler */														\
    /*TODO*///	else																				\
    /*TODO*///	{																					\
    /*TODO*///		int shift = 8 * (address & 2);													\
    /*TODO*///		read32_handler handler = (read32_handler)handlist[entry].handler;				\
    /*TODO*///		MEMREADEND((*handler)(address >> 2, ~(0xffff << shift)) >> shift)				\
    /*TODO*///	}																					\
    /*TODO*///	return 0;																			\
    /*TODO*///}																						\
    /*TODO*///
    /*TODO*///
    /*TODO*////*-------------------------------------------------
    /*TODO*///	READLONG - generic dword-sized read handler
    /*TODO*///	(32-bit aligned only!)
    /*TODO*///-------------------------------------------------*/
    /*TODO*///
    /*TODO*///#define READLONG32(name,abits,lookup,handlist,mask)										\
    /*TODO*///data32_t name(offs_t address)															\
    /*TODO*///{																						\
    /*TODO*///	UINT8 entry;																		\
    /*TODO*///	MEMREADSTART																		\
    /*TODO*///																						\
    /*TODO*///	/* perform lookup */																\
    /*TODO*///	address &= mask;																	\
    /*TODO*///	entry = lookup[LEVEL1_INDEX(address,abits,2)];										\
    /*TODO*///	if (entry >= SUBTABLE_BASE)															\
    /*TODO*///		entry = lookup[LEVEL2_INDEX(entry,address,abits,2)];							\
    /*TODO*///																						\
    /*TODO*///	/* handle banks inline */															\
    /*TODO*///	address -= handlist[entry].offset;													\
    /*TODO*///	if (entry <= STATIC_RAM)															\
    /*TODO*///		MEMREADEND(*(data32_t *)&cpu_bankbase[entry][address])							\
    /*TODO*///																						\
    /*TODO*///	/* fall back to the handler */														\
    /*TODO*///	else																				\
    /*TODO*///	{																					\
    /*TODO*///		read32_handler handler = (read32_handler)handlist[entry].handler;				\
    /*TODO*///		MEMREADEND((*handler)(address >> 2,0))										 	\
    /*TODO*///	}																					\
    /*TODO*///	return 0;																			\
    /*TODO*///}																						\
    /*TODO*///
    /*TODO*///
    /*TODO*////*-------------------------------------------------
    /*TODO*///	WRITEBYTE - generic byte-sized write handler
    /*TODO*///-------------------------------------------------*/
    /*TODO*///
    /*TODO*///#define WRITEBYTE8(name,abits,lookup,handlist,mask)										\
    /*TODO*///void name(offs_t address, data8_t data)													\
    /*TODO*///{																						\
    /*TODO*///	UINT8 entry;																		\
    /*TODO*///	MEMWRITESTART																		\
    /*TODO*///																						\
    /*TODO*///	/* perform lookup */																\
    /*TODO*///	address &= mask;																	\
    /*TODO*///	entry = lookup[LEVEL1_INDEX(address,abits,0)];										\
    /*TODO*///	if (entry >= SUBTABLE_BASE)															\
    /*TODO*///		entry = lookup[LEVEL2_INDEX(entry,address,abits,0)];							\
    /*TODO*///																						\
    /*TODO*///	/* for compatibility with setbankhandler, 8-bit systems */							\
    /*TODO*///	/* must call handlers for banks */													\
    /*TODO*///	if (entry == (FPTR)MRA_RAM)															\
    /*TODO*///		MEMWRITEEND(cpu_bankbase[STATIC_RAM][address] = data)							\
    /*TODO*///																						\
    /*TODO*///	/* fall back to the handler */														\
    /*TODO*///	else																				\
    /*TODO*///	{																					\
    /*TODO*///		write8_handler handler = (write8_handler)handlist[entry].handler;				\
    /*TODO*///		MEMWRITEEND((*handler)(address - handlist[entry].offset, data))					\
    /*TODO*///	}																					\
    /*TODO*///}																						\
    /*TODO*///
    /*TODO*///#define WRITEBYTE16BE(name,abits,lookup,handlist,mask)									\
    /*TODO*///void name(offs_t address, data8_t data)													\
    /*TODO*///{																						\
    /*TODO*///	UINT8 entry;																		\
    /*TODO*///	MEMWRITESTART																		\
    /*TODO*///																						\
    /*TODO*///	/* perform lookup */																\
    /*TODO*///	address &= mask;																	\
    /*TODO*///	entry = lookup[LEVEL1_INDEX(address,abits,1)];										\
    /*TODO*///	if (entry >= SUBTABLE_BASE)															\
    /*TODO*///		entry = lookup[LEVEL2_INDEX(entry,address,abits,1)];							\
    /*TODO*///																						\
    /*TODO*///	/* handle banks inline */															\
    /*TODO*///	address -= handlist[entry].offset;													\
    /*TODO*///	if (entry <= STATIC_RAM)															\
    /*TODO*///		MEMWRITEEND(cpu_bankbase[entry][BYTE_XOR_BE(address)] = data)					\
    /*TODO*///																						\
    /*TODO*///	/* fall back to the handler */														\
    /*TODO*///	else																				\
    /*TODO*///	{																					\
    /*TODO*///		int shift = 8 * (~address & 1);													\
    /*TODO*///		write16_handler handler = (write16_handler)handlist[entry].handler;				\
    /*TODO*///		MEMWRITEEND((*handler)(address >> 1, data << shift, ~(0xff << shift))) 			\
    /*TODO*///	}																					\
    /*TODO*///}																						\
    /*TODO*///
    /*TODO*///#define WRITEBYTE16LE(name,abits,lookup,handlist,mask)									\
    /*TODO*///void name(offs_t address, data8_t data)													\
    /*TODO*///{																						\
    /*TODO*///	UINT8 entry;																		\
    /*TODO*///	MEMWRITESTART																		\
    /*TODO*///																						\
    /*TODO*///	/* perform lookup */																\
    /*TODO*///	address &= mask;																	\
    /*TODO*///	entry = lookup[LEVEL1_INDEX(address,abits,1)];										\
    /*TODO*///	if (entry >= SUBTABLE_BASE)															\
    /*TODO*///		entry = lookup[LEVEL2_INDEX(entry,address,abits,1)];							\
    /*TODO*///																						\
    /*TODO*///	/* handle banks inline */															\
    /*TODO*///	address -= handlist[entry].offset;													\
    /*TODO*///	if (entry <= STATIC_RAM)															\
    /*TODO*///		MEMWRITEEND(cpu_bankbase[entry][BYTE_XOR_LE(address)] = data)					\
    /*TODO*///																						\
    /*TODO*///	/* fall back to the handler */														\
    /*TODO*///	else																				\
    /*TODO*///	{																					\
    /*TODO*///		int shift = 8 * (address & 1);													\
    /*TODO*///		write16_handler handler = (write16_handler)handlist[entry].handler;				\
    /*TODO*///		MEMWRITEEND((*handler)(address >> 1, data << shift, ~(0xff << shift)))			\
    /*TODO*///	}																					\
    /*TODO*///}																						\
    /*TODO*///
    /*TODO*///#define WRITEBYTE32BE(name,abits,lookup,handlist,mask)									\
    /*TODO*///void name(offs_t address, data8_t data)													\
    /*TODO*///{																						\
    /*TODO*///	UINT8 entry;																		\
    /*TODO*///	MEMWRITESTART																		\
    /*TODO*///																						\
    /*TODO*///	/* perform lookup */																\
    /*TODO*///	address &= mask;																	\
    /*TODO*///	entry = lookup[LEVEL1_INDEX(address,abits,2)];										\
    /*TODO*///	if (entry >= SUBTABLE_BASE)															\
    /*TODO*///		entry = lookup[LEVEL2_INDEX(entry,address,abits,2)];							\
    /*TODO*///																						\
    /*TODO*///	/* handle banks inline */															\
    /*TODO*///	address -= handlist[entry].offset;													\
    /*TODO*///	if (entry <= STATIC_RAM)															\
    /*TODO*///		MEMWRITEEND(cpu_bankbase[entry][BYTE4_XOR_BE(address)] = data)					\
    /*TODO*///																						\
    /*TODO*///	/* fall back to the handler */														\
    /*TODO*///	else																				\
    /*TODO*///	{																					\
    /*TODO*///		int shift = 8 * (~address & 3);													\
    /*TODO*///		write32_handler handler = (write32_handler)handlist[entry].handler;				\
    /*TODO*///		MEMWRITEEND((*handler)(address >> 2, data << shift, ~(0xff << shift))) 			\
    /*TODO*///	}																					\
    /*TODO*///}																						\
    /*TODO*///
    /*TODO*///#define WRITEBYTE32LE(name,abits,lookup,handlist,mask)									\
    /*TODO*///void name(offs_t address, data8_t data)													\
    /*TODO*///{																						\
    /*TODO*///	UINT8 entry;																		\
    /*TODO*///	MEMWRITESTART																		\
    /*TODO*///																						\
    /*TODO*///	/* perform lookup */																\
    /*TODO*///	address &= mask;																	\
    /*TODO*///	entry = lookup[LEVEL1_INDEX(address,abits,2)];										\
    /*TODO*///	if (entry >= SUBTABLE_BASE)															\
    /*TODO*///		entry = lookup[LEVEL2_INDEX(entry,address,abits,2)];							\
    /*TODO*///																						\
    /*TODO*///	/* handle banks inline */															\
    /*TODO*///	address -= handlist[entry].offset;													\
    /*TODO*///	if (entry <= STATIC_RAM)															\
    /*TODO*///		MEMWRITEEND(cpu_bankbase[entry][BYTE4_XOR_LE(address)] = data)					\
    /*TODO*///																						\
    /*TODO*///	/* fall back to the handler */														\
    /*TODO*///	else																				\
    /*TODO*///	{																					\
    /*TODO*///		int shift = 8 * (address & 3);													\
    /*TODO*///		write32_handler handler = (write32_handler)handlist[entry].handler;				\
    /*TODO*///		MEMWRITEEND((*handler)(address >> 2, data << shift, ~(0xff << shift))) 			\
    /*TODO*///	}																					\
    /*TODO*///}																						\
    /*TODO*///
    /*TODO*///
    /*TODO*////*-------------------------------------------------
    /*TODO*///	WRITEWORD - generic word-sized write handler
    /*TODO*///	(16-bit and 32-bit aligned only!)
    /*TODO*///-------------------------------------------------*/
    /*TODO*///
    /*TODO*///#define WRITEWORD16(name,abits,lookup,handlist,mask)									\
    /*TODO*///void name(offs_t address, data16_t data)												\
    /*TODO*///{																						\
    /*TODO*///	UINT8 entry;																		\
    /*TODO*///	MEMWRITESTART																		\
    /*TODO*///																						\
    /*TODO*///	/* perform lookup */																\
    /*TODO*///	address &= mask;																	\
    /*TODO*///	entry = lookup[LEVEL1_INDEX(address,abits,1)];										\
    /*TODO*///	if (entry >= SUBTABLE_BASE)															\
    /*TODO*///		entry = lookup[LEVEL2_INDEX(entry,address,abits,1)];							\
    /*TODO*///																						\
    /*TODO*///	/* handle banks inline */															\
    /*TODO*///	address -= handlist[entry].offset;													\
    /*TODO*///	if (entry <= STATIC_RAM)															\
    /*TODO*///		MEMWRITEEND(*(data16_t *)&cpu_bankbase[entry][address] = data)					\
    /*TODO*///																						\
    /*TODO*///	/* fall back to the handler */														\
    /*TODO*///	else																				\
    /*TODO*///	{																					\
    /*TODO*///		write16_handler handler = (write16_handler)handlist[entry].handler;				\
    /*TODO*///		MEMWRITEEND((*handler)(address >> 1, data, 0))								 	\
    /*TODO*///	}																					\
    /*TODO*///}																						\
    /*TODO*///
    /*TODO*///#define WRITEWORD32BE(name,abits,lookup,handlist,mask)									\
    /*TODO*///void name(offs_t address, data16_t data)												\
    /*TODO*///{																						\
    /*TODO*///	UINT8 entry;																		\
    /*TODO*///	MEMWRITESTART																		\
    /*TODO*///																						\
    /*TODO*///	/* perform lookup */																\
    /*TODO*///	address &= mask;																	\
    /*TODO*///	entry = lookup[LEVEL1_INDEX(address,abits,2)];										\
    /*TODO*///	if (entry >= SUBTABLE_BASE)															\
    /*TODO*///		entry = lookup[LEVEL2_INDEX(entry,address,abits,2)];							\
    /*TODO*///																						\
    /*TODO*///	/* handle banks inline */															\
    /*TODO*///	address -= handlist[entry].offset;													\
    /*TODO*///	if (entry <= STATIC_RAM)															\
    /*TODO*///		MEMWRITEEND(*(data16_t *)&cpu_bankbase[entry][WORD_XOR_BE(address)] = data)		\
    /*TODO*///																						\
    /*TODO*///	/* fall back to the handler */														\
    /*TODO*///	else																				\
    /*TODO*///	{																					\
    /*TODO*///		int shift = 8 * (~address & 2);													\
    /*TODO*///		write32_handler handler = (write32_handler)handlist[entry].handler;				\
    /*TODO*///		MEMWRITEEND((*handler)(address >> 2, data << shift, ~(0xffff << shift))) 		\
    /*TODO*///	}																					\
    /*TODO*///}																						\
    /*TODO*///
    /*TODO*///#define WRITEWORD32LE(name,abits,lookup,handlist,mask)									\
    /*TODO*///void name(offs_t address, data16_t data)												\
    /*TODO*///{																						\
    /*TODO*///	UINT8 entry;																		\
    /*TODO*///	MEMWRITESTART																		\
    /*TODO*///																						\
    /*TODO*///	/* perform lookup */																\
    /*TODO*///	address &= mask;																	\
    /*TODO*///	entry = lookup[LEVEL1_INDEX(address,abits,2)];										\
    /*TODO*///	if (entry >= SUBTABLE_BASE)															\
    /*TODO*///		entry = lookup[LEVEL2_INDEX(entry,address,abits,2)];							\
    /*TODO*///																						\
    /*TODO*///	/* handle banks inline */															\
    /*TODO*///	address -= handlist[entry].offset;													\
    /*TODO*///	if (entry <= STATIC_RAM)															\
    /*TODO*///		MEMWRITEEND(*(data16_t *)&cpu_bankbase[entry][WORD_XOR_LE(address)] = data)		\
    /*TODO*///																						\
    /*TODO*///	/* fall back to the handler */														\
    /*TODO*///	else																				\
    /*TODO*///	{																					\
    /*TODO*///		int shift = 8 * (address & 2);													\
    /*TODO*///		write32_handler handler = (write32_handler)handlist[entry].handler;				\
    /*TODO*///		MEMWRITEEND((*handler)(address >> 2, data << shift, ~(0xffff << shift))) 		\
    /*TODO*///	}																					\
    /*TODO*///}																						\
    /*TODO*///
    /*TODO*///
    /*TODO*////*-------------------------------------------------
    /*TODO*///	WRITELONG - dword-sized write handler
    /*TODO*///	(32-bit aligned only!)
    /*TODO*///-------------------------------------------------*/
    /*TODO*///
    /*TODO*///#define WRITELONG32(name,abits,lookup,handlist,mask)									\
    /*TODO*///void name(offs_t address, data32_t data)												\
    /*TODO*///{																						\
    /*TODO*///	UINT8 entry;																		\
    /*TODO*///	MEMWRITESTART																		\
    /*TODO*///																						\
    /*TODO*///	/* perform lookup */																\
    /*TODO*///	address &= mask;																	\
    /*TODO*///	entry = lookup[LEVEL1_INDEX(address,abits,2)];										\
    /*TODO*///	if (entry >= SUBTABLE_BASE)															\
    /*TODO*///		entry = lookup[LEVEL2_INDEX(entry,address,abits,2)];							\
    /*TODO*///																						\
    /*TODO*///	/* handle banks inline */															\
    /*TODO*///	address -= handlist[entry].offset;													\
    /*TODO*///	if (entry <= STATIC_RAM)															\
    /*TODO*///		MEMWRITEEND(*(data32_t *)&cpu_bankbase[entry][address] = data)					\
    /*TODO*///																						\
    /*TODO*///	/* fall back to the handler */														\
    /*TODO*///	else																				\
    /*TODO*///	{																					\
    /*TODO*///		write32_handler handler = (write32_handler)handlist[entry].handler;				\
    /*TODO*///		MEMWRITEEND((*handler)(address >> 2, data, 0))								 	\
    /*TODO*///	}																					\
    /*TODO*///}																						\
    /*TODO*///
    /*TODO*///
    /*TODO*////*-------------------------------------------------
    /*TODO*///	SETOPBASE - generic opcode base changer
    /*TODO*///-------------------------------------------------*/
    /*TODO*///
    /*TODO*///#define SETOPBASE(name,abits,minbits,table)												\
    /*TODO*///void name(offs_t pc)																	\
    /*TODO*///{																						\
    /*TODO*///	UINT8 *base;																		\
    /*TODO*///	UINT8 entry;																		\
    /*TODO*///																						\
    /*TODO*///	/* allow overrides */																\
    /*TODO*///	if (opbasefunc) 																	\
    /*TODO*///	{																					\
    /*TODO*///		pc = (*opbasefunc)(pc);															\
    /*TODO*///		if (pc == ~0)																	\
    /*TODO*///			return; 																	\
    /*TODO*///	}																					\
    /*TODO*///																						\
    /*TODO*///	/* perform the lookup */															\
    /*TODO*///	pc &= mem_amask;																	\
    /*TODO*///	entry = readmem_lookup[LEVEL1_INDEX(pc,abits,minbits)];								\
    /*TODO*///	if (entry >= SUBTABLE_BASE)															\
    /*TODO*///		entry = readmem_lookup[LEVEL2_INDEX(entry,pc,abits,minbits)];					\
    /*TODO*///	opcode_entry = entry;																\
    /*TODO*///																						\
    /*TODO*///	/* RAM/ROM/RAMROM */																\
    /*TODO*///	if (entry >= STATIC_RAM && entry <= STATIC_RAMROM)									\
    /*TODO*///		base = cpu_bankbase[STATIC_RAM];												\
    /*TODO*///																						\
    /*TODO*///	/* banked memory */																	\
    /*TODO*///	else if (entry >= STATIC_BANK1 && entry <= STATIC_RAM)								\
    /*TODO*///		base = cpu_bankbase[entry];														\
    /*TODO*///																						\
    /*TODO*///	/* other memory -- could be very slow! */											\
    /*TODO*///	else																				\
    /*TODO*///	{																					\
    /*TODO*///		logerror("cpu #%d (PC=%08X): warning - op-code execute on mapped I/O\n",		\
    /*TODO*///					cpu_getactivecpu(), activecpu_get_pc());							\
    /*TODO*///		/*base = memory_find_base(cpu_getactivecpu(), pc);*/							\
    /*TODO*///		return;																			\
    /*TODO*///	}																					\
    /*TODO*///																						\
    /*TODO*///	/* compute the adjusted base */														\
    /*TODO*///	OP_ROM = base - table[entry].offset + (OP_ROM - OP_RAM);							\
    /*TODO*///	OP_RAM = base - table[entry].offset;												\
    /*TODO*///}
    /*TODO*///
    /*TODO*///
    /*TODO*////*-------------------------------------------------
    /*TODO*///	GENERATE_HANDLERS - macros to spew out all
    /*TODO*///	the handlers needed for a given memory type
    /*TODO*///-------------------------------------------------*/
    /*TODO*///
    public static setopbase cpu_setOPbase16 = new setopbase() {
        public void handler(int pc) {
            UBytePtr base = null;
            int entry;

            /* allow overrides */
            if (opbasefunc != null) {
                throw new UnsupportedOperationException("Unsupported");
                /*TODO*///		pc = (*opbasefunc)(pc);															
/*TODO*///		if (pc == ~0)																	
/*TODO*///			return; 
            }

            /* perform the lookup */
            pc &= mem_amask;
            entry = readmem_lookup.read(LEVEL1_INDEX(pc, 16, 0));
            if (entry >= SUBTABLE_BASE) {
                entry = readmem_lookup.read(LEVEL2_INDEX(entry, pc, 16, 0));
            }
            opcode_entry = entry;
            
            /* RAM/ROM/RAMROM */
            if (entry >= STATIC_RAM && entry <= STATIC_RAMROM) {
                base = new UBytePtr(cpu_bankbase[STATIC_RAM]);
            } /* banked memory */ else if (entry >= STATIC_BANK1 && entry <= STATIC_RAM) {
                if (cpu_bankbase[entry] != null){
                    base = new UBytePtr(cpu_bankbase[entry]);
                }
            } /* other memory -- could be very slow! */ else {
                logerror("cpu #%d (PC=%08X): warning - op-code execute on mapped I/O\n", cpu_getactivecpu(), activecpu_get_pc());
                /*base = memory_find_base(cpu_getactivecpu(), pc);*/
                return;
            }

            /* compute the adjusted base */
            OP_ROM = new UBytePtr(base, -rmemhandler8[entry].offset + (OP_ROM.offset - OP_RAM.offset));
            OP_RAM = new UBytePtr(base, -rmemhandler8[entry].offset);
        }
    };

    public static int cpu_readmem16(int address) {
        int entry;
        /* perform lookup */
        address &= mem_amask;
        entry = readmem_lookup.read(LEVEL1_INDEX(address, 16, 0));
        if (entry >= SUBTABLE_BASE) {
            entry = readmem_lookup.read(LEVEL2_INDEX(entry, address, 16, 0));
        }

        /* for compatibility with setbankhandler, 8-bit systems */
 /* must call handlers for banks */
        if (entry == STATIC_RAM) {
            return cpu_bankbase[STATIC_RAM].read(address);
        } /* fall back to the handler */ else {
            ReadHandlerPtr handler = (ReadHandlerPtr) rmemhandler8[entry].handler;
            return handler.handler(address - rmemhandler8[entry].offset);
        }
        //return 0;
    };

    public static void cpu_writemem16(int address, int data) {
        int entry;

        /* perform lookup */
        address &= mem_amask;
        entry = writemem_lookup.read(LEVEL1_INDEX(address, 16, 0));
        if (entry >= SUBTABLE_BASE) {
            entry = writemem_lookup.read(LEVEL2_INDEX(entry, address, 16, 0));
        }

        /* for compatibility with setbankhandler, 8-bit systems */
 /* must call handlers for banks */
        if (entry == MRA_RAM) {
            cpu_bankbase[STATIC_RAM].write(address, data);
        } /* fall back to the handler */ else {
            WriteHandlerPtr handler = (WriteHandlerPtr) wmemhandler8[entry].handler;
            handler.handler(address - wmemhandler8[entry].offset, data);
        }
    };

    public static int cpu_readport16(int address) {
        int entry;
        /* perform lookup */
        address &= port_amask;
        entry = readport_lookup.read(LEVEL1_INDEX(address, 16, 0));
        if (entry >= SUBTABLE_BASE) {
            entry = readport_lookup.read(LEVEL2_INDEX(entry, address, 16, 0));
        }

        /* for compatibility with setbankhandler, 8-bit systems */
 /* must call handlers for banks */
        if (entry == STATIC_RAM) {
            return cpu_bankbase[STATIC_RAM].read(address);
        } /* fall back to the handler */ else {
            ReadHandlerPtr handler = (ReadHandlerPtr) rporthandler8[entry].handler;
            return handler.handler(address - rporthandler8[entry].offset);
        }
    };

    public static void cpu_writeport16(int address, int data) {
        int entry;
        /* perform lookup */
        address &= port_amask;
        entry = writeport_lookup.read(LEVEL1_INDEX(address, 16, 0));
        if (entry >= SUBTABLE_BASE) {
            entry = writeport_lookup.read(LEVEL2_INDEX(entry, address, 16, 0));
        }

        /* for compatibility with setbankhandler, 8-bit systems */
 /* must call handlers for banks */
        if (entry == MRA_RAM) {
            cpu_bankbase[STATIC_RAM].write(address, data);
        } /* fall back to the handler */ else {
            WriteHandlerPtr handler = (WriteHandlerPtr) wporthandler8[entry].handler;
            handler.handler(address - wporthandler8[entry].offset, data);
        }
    };
    
    /*---------------------------------------------*/
    
    public static setopbase cpu_setOPbase24 = new setopbase() {
        public void handler(int pc) {
            UBytePtr base = null;
            int entry;

            /* allow overrides */
            if (opbasefunc != null) {
                throw new UnsupportedOperationException("Unsupported");
                /*TODO*///		pc = (*opbasefunc)(pc);															
/*TODO*///		if (pc == ~0)																	
/*TODO*///			return; 
            }

            /* perform the lookup */
            pc &= mem_amask;
            entry = readmem_lookup.read(LEVEL1_INDEX(pc, 24, 0));
            if (entry >= SUBTABLE_BASE) {
                entry = readmem_lookup.read(LEVEL2_INDEX(entry, pc, 24, 0));
            }
            opcode_entry = entry;
            
            /* RAM/ROM/RAMROM */
            if (entry >= STATIC_RAM && entry <= STATIC_RAMROM) {
                base = new UBytePtr(cpu_bankbase[STATIC_RAM]);
            } /* banked memory */ else if (entry >= STATIC_BANK1 && entry <= STATIC_RAM) {
                if (cpu_bankbase[entry] != null){
                    base = new UBytePtr(cpu_bankbase[entry]);
                }
            } /* other memory -- could be very slow! */ else {
                logerror("cpu #%d (PC=%08X): warning - op-code execute on mapped I/O\n", cpu_getactivecpu(), activecpu_get_pc());
                /*base = memory_find_base(cpu_getactivecpu(), pc);*/
                return;
            }

            /* compute the adjusted base */
            OP_ROM = new UBytePtr(base, -rmemhandler8[entry].offset + (OP_ROM.offset - OP_RAM.offset));
            OP_RAM = new UBytePtr(base, -rmemhandler8[entry].offset);
        }
    };

    public static int cpu_readmem24(int address) {
        int entry;
        /* perform lookup */
        address &= mem_amask;
        entry = readmem_lookup.read(LEVEL1_INDEX(address, 24, 0));
        if (entry >= SUBTABLE_BASE) {
            entry = readmem_lookup.read(LEVEL2_INDEX(entry, address, 24, 0));
        }

        /* for compatibility with setbankhandler, 8-bit systems */
 /* must call handlers for banks */
        if (entry == STATIC_RAM) {
            return cpu_bankbase[STATIC_RAM].read(address);
        } /* fall back to the handler */ else {
            ReadHandlerPtr handler = (ReadHandlerPtr) rmemhandler8[entry].handler;
            return handler.handler(address - rmemhandler8[entry].offset);
        }
        //return 0;
    }

    public static void cpu_writemem24(int address, int data) {
        int entry;

        /* perform lookup */
        address &= mem_amask;
        entry = writemem_lookup.read(LEVEL1_INDEX(address, 24, 0));
        if (entry >= SUBTABLE_BASE) {
            entry = writemem_lookup.read(LEVEL2_INDEX(entry, address, 24, 0));
        }

        /* for compatibility with setbankhandler, 8-bit systems */
 /* must call handlers for banks */
        if (entry == MRA_RAM) {
            cpu_bankbase[STATIC_RAM].write(address, data);
        } /* fall back to the handler */ else {
            WriteHandlerPtr handler = (WriteHandlerPtr) wmemhandler8[entry].handler;
            handler.handler(address - wmemhandler8[entry].offset, data);
        }
    }

    public static int cpu_readport24(int address) {
        int entry;
        /* perform lookup */
        address &= port_amask;
        entry = readport_lookup.read(LEVEL1_INDEX(address, 24, 0));
        if (entry >= SUBTABLE_BASE) {
            entry = readport_lookup.read(LEVEL2_INDEX(entry, address, 24, 0));
        }

        /* for compatibility with setbankhandler, 8-bit systems */
 /* must call handlers for banks */
        if (entry == STATIC_RAM) {
            return cpu_bankbase[STATIC_RAM].read(address);
        } /* fall back to the handler */ else {
            ReadHandlerPtr handler = (ReadHandlerPtr) rporthandler8[entry].handler;
            return handler.handler(address - rporthandler8[entry].offset);
        }
    }

    public static void cpu_writeport24(int address, int data) {
        int entry;
        /* perform lookup */
        address &= port_amask;
        entry = writeport_lookup.read(LEVEL1_INDEX(address, 24, 0));
        if (entry >= SUBTABLE_BASE) {
            entry = writeport_lookup.read(LEVEL2_INDEX(entry, address, 24, 0));
        }

        /* for compatibility with setbankhandler, 8-bit systems */
 /* must call handlers for banks */
        if (entry == MRA_RAM) {
            cpu_bankbase[STATIC_RAM].write(address, data);
        } /* fall back to the handler */ else {
            WriteHandlerPtr handler = (WriteHandlerPtr) wporthandler8[entry].handler;
            handler.handler(address - wporthandler8[entry].offset, data);
        }
    }

    /*TODO*///#define GENERATE_HANDLERS_8BIT(type, abits) \
    /*TODO*///	    READBYTE8(cpu_read##type##abits,             abits, read##type##_lookup,  r##type##handler8,  type##_amask) \
    /*TODO*///	   WRITEBYTE8(cpu_write##type##abits,            abits, write##type##_lookup, w##type##handler8,  type##_amask)
    /*TODO*///
    /*TODO*///#define GENERATE_HANDLERS_16BIT_BE(type, abits) \
    /*TODO*///	 READBYTE16BE(cpu_read##type##abits##bew,        abits, read##type##_lookup,  r##type##handler16, type##_amask) \
    /*TODO*///	   READWORD16(cpu_read##type##abits##bew_word,   abits, read##type##_lookup,  r##type##handler16, type##_amask) \
    /*TODO*///	WRITEBYTE16BE(cpu_write##type##abits##bew,       abits, write##type##_lookup, w##type##handler16, type##_amask) \
    /*TODO*///	  WRITEWORD16(cpu_write##type##abits##bew_word,  abits, write##type##_lookup, w##type##handler16, type##_amask)
    /*TODO*///
    /*TODO*///#define GENERATE_HANDLERS_16BIT_LE(type, abits) \
    /*TODO*///	 READBYTE16LE(cpu_read##type##abits##lew,        abits, read##type##_lookup,  r##type##handler16, type##_amask) \
    /*TODO*///	   READWORD16(cpu_read##type##abits##lew_word,   abits, read##type##_lookup,  r##type##handler16, type##_amask) \
    /*TODO*///	WRITEBYTE16LE(cpu_write##type##abits##lew,       abits, write##type##_lookup, w##type##handler16, type##_amask) \
    /*TODO*///	  WRITEWORD16(cpu_write##type##abits##lew_word,  abits, write##type##_lookup, w##type##handler16, type##_amask)
    /*TODO*///
    /*TODO*///#define GENERATE_HANDLERS_32BIT_BE(type, abits) \
    /*TODO*///	 READBYTE32BE(cpu_read##type##abits##bedw,       abits, read##type##_lookup,  r##type##handler32, type##_amask) \
    /*TODO*///	 READWORD32BE(cpu_read##type##abits##bedw_word,  abits, read##type##_lookup,  r##type##handler32, type##_amask) \
    /*TODO*///	   READLONG32(cpu_read##type##abits##bedw_dword, abits, read##type##_lookup,  r##type##handler32, type##_amask) \
    /*TODO*///	WRITEBYTE32BE(cpu_write##type##abits##bedw,      abits, write##type##_lookup, w##type##handler32, type##_amask) \
    /*TODO*///	WRITEWORD32BE(cpu_write##type##abits##bedw_word, abits, write##type##_lookup, w##type##handler32, type##_amask) \
    /*TODO*///	  WRITELONG32(cpu_write##type##abits##bedw_dword,abits, write##type##_lookup, w##type##handler32, type##_amask)
    /*TODO*///
    /*TODO*///#define GENERATE_HANDLERS_32BIT_LE(type, abits) \
    /*TODO*///	 READBYTE32LE(cpu_read##type##abits##ledw,       abits, read##type##_lookup,  r##type##handler32, type##_amask) \
    /*TODO*///	 READWORD32LE(cpu_read##type##abits##ledw_word,  abits, read##type##_lookup,  r##type##handler32, type##_amask) \
    /*TODO*///	   READLONG32(cpu_read##type##abits##ledw_dword, abits, read##type##_lookup,  r##type##handler32, type##_amask) \
    /*TODO*///	WRITEBYTE32LE(cpu_write##type##abits##ledw,      abits, write##type##_lookup, w##type##handler32, type##_amask) \
    /*TODO*///	WRITEWORD32LE(cpu_write##type##abits##ledw_word, abits, write##type##_lookup, w##type##handler32, type##_amask) \
    /*TODO*///	  WRITELONG32(cpu_write##type##abits##ledw_dword,abits, write##type##_lookup, w##type##handler32, type##_amask)
    /*TODO*///
    /*TODO*///
    /*TODO*////*-------------------------------------------------
    /*TODO*///	GENERATE_MEM_HANDLERS - memory handler
    /*TODO*///	variants of the GENERATE_HANDLERS
    /*TODO*///-------------------------------------------------*/
    /*TODO*///
    /*TODO*///#define GENERATE_MEM_HANDLERS_8BIT(abits) \
    /*TODO*///GENERATE_HANDLERS_8BIT(mem, abits) \
    /*TODO*///SETOPBASE(cpu_setopbase##abits,           abits, 0, rmemhandler8)
    /*TODO*///
    /*TODO*///#define GENERATE_MEM_HANDLERS_16BIT_BE(abits) \
    /*TODO*///GENERATE_HANDLERS_16BIT_BE(mem, abits) \
    /*TODO*///SETOPBASE(cpu_setopbase##abits##bew,      abits, 1, rmemhandler16)
    /*TODO*///
    /*TODO*///#define GENERATE_MEM_HANDLERS_16BIT_LE(abits) \
    /*TODO*///GENERATE_HANDLERS_16BIT_LE(mem, abits) \
    /*TODO*///SETOPBASE(cpu_setopbase##abits##lew,      abits, 1, rmemhandler16)
    /*TODO*///
    /*TODO*///#define GENERATE_MEM_HANDLERS_32BIT_BE(abits) \
    /*TODO*///GENERATE_HANDLERS_32BIT_BE(mem, abits) \
    /*TODO*///SETOPBASE(cpu_setopbase##abits##bedw,     abits, 2, rmemhandler32)
    /*TODO*///
    /*TODO*///#define GENERATE_MEM_HANDLERS_32BIT_LE(abits) \
    /*TODO*///GENERATE_HANDLERS_32BIT_LE(mem, abits) \
    /*TODO*///SETOPBASE(cpu_setopbase##abits##ledw,     abits, 2, rmemhandler32)
    /*TODO*///
    /*TODO*///
    /*TODO*////*-------------------------------------------------
    /*TODO*///	GENERATE_PORT_HANDLERS - port handler
    /*TODO*///	variants of the GENERATE_HANDLERS
    /*TODO*///-------------------------------------------------*/
    /*TODO*///
    /*TODO*///#define GENERATE_PORT_HANDLERS_8BIT(abits) \
    /*TODO*///GENERATE_HANDLERS_8BIT(port, abits)
    /*TODO*///
    /*TODO*///#define GENERATE_PORT_HANDLERS_16BIT_BE(abits) \
    /*TODO*///GENERATE_HANDLERS_16BIT_BE(port, abits)
    /*TODO*///
    /*TODO*///#define GENERATE_PORT_HANDLERS_16BIT_LE(abits) \
    /*TODO*///GENERATE_HANDLERS_16BIT_LE(port, abits)
    /*TODO*///
    /*TODO*///#define GENERATE_PORT_HANDLERS_32BIT_BE(abits) \
    /*TODO*///GENERATE_HANDLERS_32BIT_BE(port, abits)
    /*TODO*///
    /*TODO*///#define GENERATE_PORT_HANDLERS_32BIT_LE(abits) \
    /*TODO*///GENERATE_HANDLERS_32BIT_LE(port, abits)
    /*TODO*///
    /*TODO*///
    /*TODO*////*-------------------------------------------------
    /*TODO*///	the memory handlers we need to generate
    /*TODO*///-------------------------------------------------*/
    /*TODO*///
    /*TODO*///GENERATE_MEM_HANDLERS_8BIT(16)
    /*TODO*///GENERATE_MEM_HANDLERS_8BIT(20)
    /*TODO*///GENERATE_MEM_HANDLERS_8BIT(21)
    /*TODO*///GENERATE_MEM_HANDLERS_8BIT(24)
    /*TODO*///
    /*TODO*///GENERATE_MEM_HANDLERS_16BIT_BE(16)
    /*TODO*///GENERATE_MEM_HANDLERS_16BIT_BE(24)
    /*TODO*///GENERATE_MEM_HANDLERS_16BIT_BE(32)
    /*TODO*///
    /*TODO*///GENERATE_MEM_HANDLERS_16BIT_LE(16)
    /*TODO*///GENERATE_MEM_HANDLERS_16BIT_LE(17)
    /*TODO*///GENERATE_MEM_HANDLERS_16BIT_LE(24)
    /*TODO*///GENERATE_MEM_HANDLERS_16BIT_LE(29)
    /*TODO*///GENERATE_MEM_HANDLERS_16BIT_LE(32)
    /*TODO*///
    /*TODO*///GENERATE_MEM_HANDLERS_32BIT_BE(24)
    /*TODO*///GENERATE_MEM_HANDLERS_32BIT_BE(29)
    /*TODO*///GENERATE_MEM_HANDLERS_32BIT_BE(32)
    /*TODO*///
    /*TODO*///GENERATE_MEM_HANDLERS_32BIT_LE(26)
    /*TODO*///GENERATE_MEM_HANDLERS_32BIT_LE(29)
    /*TODO*///GENERATE_MEM_HANDLERS_32BIT_LE(32)
    /*TODO*///
    /*TODO*///GENERATE_MEM_HANDLERS_32BIT_BE(18)	/* HACK -- used for pdp-1 */
    /*TODO*///
    /*TODO*///
    /*TODO*///
    /*TODO*////*-------------------------------------------------
    /*TODO*///	the port handlers we need to generate
    /*TODO*///-------------------------------------------------*/
    /*TODO*///
    /*TODO*///GENERATE_PORT_HANDLERS_8BIT(16)
    /*TODO*///
    /*TODO*///GENERATE_PORT_HANDLERS_16BIT_BE(16)
    /*TODO*///
    /*TODO*///GENERATE_PORT_HANDLERS_16BIT_LE(16)
    /*TODO*///GENERATE_PORT_HANDLERS_16BIT_LE(24)
    /*TODO*///
    /*TODO*///GENERATE_PORT_HANDLERS_32BIT_BE(16)
    /*TODO*///
    /*TODO*///GENERATE_PORT_HANDLERS_32BIT_LE(16)
    /*TODO*///GENERATE_PORT_HANDLERS_32BIT_LE(24)
    /*-------------------------------------------------
    	get address bits from a read handler
    -------------------------------------------------*/
    public static int mem_address_bits_of_cpu(int cputype) {
        return cputype_get_interface(cputype).mem_address_bits_of_cpu();
    }

    /*-------------------------------------------------
    	get address bits from a read handler
    -------------------------------------------------*/
    public static int port_address_bits_of_cpu(int cputype) {
        return cputype == CPU_V60 ? 24 : 16;
    }

    /*-------------------------------------------------
    	basic static handlers
    -------------------------------------------------*/
    public static ReadHandlerPtr mrh8_bad = new ReadHandlerPtr() {
        public int handler(int offset) {
            logerror("cpu #%d (PC=%08X): unmapped memory byte read from %08X\n", cpu_getactivecpu(), activecpu_get_pc(), offset);
            if (activecpu_address_bits() <= SPARSE_THRESH) {
                return cpu_bankbase[STATIC_RAM].read(offset);
            }
            return 0;
        }
    };

    /*TODO*///static READ16_HANDLER( mrh16_bad )
    /*TODO*///{
    /*TODO*///	logerror("cpu #%d (PC=%08X): unmapped memory word read from %08X & %04X\n", cpu_getactivecpu(), activecpu_get_pc(), offset*2, mem_mask ^ 0xffff);
    /*TODO*///	if (activecpu_address_bits() <= SPARSE_THRESH) return ((data16_t *)cpu_bankbase[STATIC_RAM])[offset];
    /*TODO*///	return 0;
    /*TODO*///}
    /*TODO*///static READ32_HANDLER( mrh32_bad )
    /*TODO*///{
    /*TODO*///	logerror("cpu #%d (PC=%08X): unmapped memory dword read from %08X & %08X\n", cpu_getactivecpu(), activecpu_get_pc(), offset*4, mem_mask ^ 0xffffffff);
    /*TODO*///	if (activecpu_address_bits() <= SPARSE_THRESH) return ((data32_t *)cpu_bankbase[STATIC_RAM])[offset];
    /*TODO*///	return 0;
    /*TODO*///}
    /*TODO*///
    public static WriteHandlerPtr mwh8_bad = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            logerror("cpu #%d (PC=%08X): unmapped memory byte write to %08X = %02X\n", cpu_getactivecpu(), activecpu_get_pc(), offset, data);
            if (activecpu_address_bits() <= SPARSE_THRESH) {
                cpu_bankbase[STATIC_RAM].write(offset, data);
            }
        }
    };
    /*TODO*///static WRITE16_HANDLER( mwh16_bad )
    /*TODO*///{
    /*TODO*///	logerror("cpu #%d (PC=%08X): unmapped memory word write to %08X = %04X & %04X\n", cpu_getactivecpu(), activecpu_get_pc(), offset*2, data, mem_mask ^ 0xffff);
    /*TODO*///	if (activecpu_address_bits() <= SPARSE_THRESH) COMBINE_DATA(&((data16_t *)cpu_bankbase[STATIC_RAM])[offset]);
    /*TODO*///}
    /*TODO*///static WRITE32_HANDLER( mwh32_bad )
    /*TODO*///{
    /*TODO*///	logerror("cpu #%d (PC=%08X): unmapped memory dword write to %08X = %08X & %08X\n", cpu_getactivecpu(), activecpu_get_pc(), offset*4, data, mem_mask ^ 0xffffffff);
    /*TODO*///	if (activecpu_address_bits() <= SPARSE_THRESH) COMBINE_DATA(&((data32_t *)cpu_bankbase[STATIC_RAM])[offset]);
    /*TODO*///}
    /*TODO*///
    public static ReadHandlerPtr prh8_bad = new ReadHandlerPtr() {
        public int handler(int offset) {
            logerror("cpu #%d (PC=%08X): unmapped port byte read from %08X\n", cpu_getactivecpu(), activecpu_get_pc(), offset);
            return 0;
        }
    };
    /*TODO*///static READ16_HANDLER( prh16_bad )
    /*TODO*///{
    /*TODO*///	logerror("cpu #%d (PC=%08X): unmapped port word read from %08X & %04X\n", cpu_getactivecpu(), activecpu_get_pc(), offset*2, mem_mask ^ 0xffff);
    /*TODO*///	return 0;
    /*TODO*///}
    /*TODO*///static READ32_HANDLER( prh32_bad )
    /*TODO*///{
    /*TODO*///	logerror("cpu #%d (PC=%08X): unmapped port dword read from %08X & %08X\n", cpu_getactivecpu(), activecpu_get_pc(), offset*4, mem_mask ^ 0xffffffff);
    /*TODO*///	return 0;
    /*TODO*///}
    /*TODO*///
    public static WriteHandlerPtr pwh8_bad = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            logerror("cpu #%d (PC=%08X): unmapped port byte write to %08X = %02X\n", cpu_getactivecpu(), activecpu_get_pc(), offset, data);
        }
    };
    /*TODO*///static WRITE16_HANDLER( pwh16_bad )
    /*TODO*///{
    /*TODO*///	logerror("cpu #%d (PC=%08X): unmapped port word write to %08X = %04X & %04X\n", cpu_getactivecpu(), activecpu_get_pc(), offset*2, data, mem_mask ^ 0xffff);
    /*TODO*///}
    /*TODO*///static WRITE32_HANDLER( pwh32_bad )
    /*TODO*///{
    /*TODO*///	logerror("cpu #%d (PC=%08X): unmapped port dword write to %08X = %08X & %08X\n", cpu_getactivecpu(), activecpu_get_pc(), offset*4, data, mem_mask ^ 0xffffffff);
    /*TODO*///}
    /*TODO*///
    public static WriteHandlerPtr mwh8_rom = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            logerror("cpu #%d (PC=%08X): byte write to ROM %08X = %02X\n", cpu_getactivecpu(), activecpu_get_pc(), offset, data);
        }
    };
    /*TODO*///static WRITE16_HANDLER( mwh16_rom )    { logerror("cpu #%d (PC=%08X): word write to %08X = %04X & %04X\n", cpu_getactivecpu(), activecpu_get_pc(), offset*2, data, mem_mask ^ 0xffff); }
    /*TODO*///static WRITE32_HANDLER( mwh32_rom )    { logerror("cpu #%d (PC=%08X): dword write to %08X = %08X & %08X\n", cpu_getactivecpu(), activecpu_get_pc(), offset*4, data, mem_mask ^ 0xffffffff); }
    /*TODO*///
    public static ReadHandlerPtr mrh8_nop = new ReadHandlerPtr() {
        public int handler(int offset) {
            return 0;
        }
    };
    /*TODO*///static READ16_HANDLER( mrh16_nop )     { return 0; }
    /*TODO*///static READ32_HANDLER( mrh32_nop )     { return 0; }
    /*TODO*///
    public static WriteHandlerPtr mwh8_nop = new WriteHandlerPtr() {
        public void handler(int offset, int data) {

        }
    };
    /*TODO*///static WRITE16_HANDLER( mwh16_nop )    {  }
    /*TODO*///static WRITE32_HANDLER( mwh32_nop )    {  }
    public static ReadHandlerPtr mrh8_ram = new ReadHandlerPtr() {
        public int handler(int offset) {
            return cpu_bankbase[STATIC_RAM].read(offset);
        }
    };
    public static WriteHandlerPtr mwh8_ram = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            cpu_bankbase[STATIC_RAM].write(offset, data);
        }
    };
    public static WriteHandlerPtr mwh8_ramrom = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            cpu_bankbase[STATIC_RAM].write(offset + (OP_ROM.offset - OP_RAM.offset), data);
            cpu_bankbase[STATIC_RAM].write(offset, data);
        }
    };

    /*TODO*///static WRITE16_HANDLER( mwh16_ramrom ) { COMBINE_DATA(&cpu_bankbase[STATIC_RAM][offset*2]); COMBINE_DATA(&cpu_bankbase[0][offset*2 + (OP_ROM - OP_RAM)]); }
    /*TODO*///static WRITE32_HANDLER( mwh32_ramrom ) { COMBINE_DATA(&cpu_bankbase[STATIC_RAM][offset*4]); COMBINE_DATA(&cpu_bankbase[0][offset*4 + (OP_ROM - OP_RAM)]); }
    /*TODO*///
    public static ReadHandlerPtr mrh8_bank1 = new ReadHandlerPtr() {
        public int handler(int offset) {
            return cpu_bankbase[1].read(offset);
        }
    };
    public static ReadHandlerPtr mrh8_bank2 = new ReadHandlerPtr() {
        public int handler(int offset) {
            return cpu_bankbase[2].read(offset);
        }
    };
    public static ReadHandlerPtr mrh8_bank3 = new ReadHandlerPtr() {
        public int handler(int offset) {
            return cpu_bankbase[3].read(offset);
        }
    };
    public static ReadHandlerPtr mrh8_bank4 = new ReadHandlerPtr() {
        public int handler(int offset) {
            return cpu_bankbase[4].read(offset);
        }
    };
    public static ReadHandlerPtr mrh8_bank5 = new ReadHandlerPtr() {
        public int handler(int offset) {
            return cpu_bankbase[5].read(offset);
        }
    };
    public static ReadHandlerPtr mrh8_bank6 = new ReadHandlerPtr() {
        public int handler(int offset) {
            return cpu_bankbase[6].read(offset);
        }
    };
    public static ReadHandlerPtr mrh8_bank7 = new ReadHandlerPtr() {
        public int handler(int offset) {
            return cpu_bankbase[7].read(offset);
        }
    };
    public static ReadHandlerPtr mrh8_bank8 = new ReadHandlerPtr() {
        public int handler(int offset) {
            return cpu_bankbase[8].read(offset);
        }
    };
    public static ReadHandlerPtr mrh8_bank9 = new ReadHandlerPtr() {
        public int handler(int offset) {
            return cpu_bankbase[9].read(offset);
        }
    };
    public static ReadHandlerPtr mrh8_bank10 = new ReadHandlerPtr() {
        public int handler(int offset) {
            return cpu_bankbase[10].read(offset);
        }
    };
    public static ReadHandlerPtr mrh8_bank11 = new ReadHandlerPtr() {
        public int handler(int offset) {
            return cpu_bankbase[11].read(offset);
        }
    };
    public static ReadHandlerPtr mrh8_bank12 = new ReadHandlerPtr() {
        public int handler(int offset) {
            return cpu_bankbase[12].read(offset);
        }
    };
    public static ReadHandlerPtr mrh8_bank13 = new ReadHandlerPtr() {
        public int handler(int offset) {
            return cpu_bankbase[13].read(offset);
        }
    };
    public static ReadHandlerPtr mrh8_bank14 = new ReadHandlerPtr() {
        public int handler(int offset) {
            return cpu_bankbase[14].read(offset);
        }
    };
    public static ReadHandlerPtr mrh8_bank15 = new ReadHandlerPtr() {
        public int handler(int offset) {
            return cpu_bankbase[15].read(offset);
        }
    };
    public static ReadHandlerPtr mrh8_bank16 = new ReadHandlerPtr() {
        public int handler(int offset) {
            return cpu_bankbase[16].read(offset);
        }
    };
    public static ReadHandlerPtr mrh8_bank17 = new ReadHandlerPtr() {
        public int handler(int offset) {
            return cpu_bankbase[17].read(offset);
        }
    };
    public static ReadHandlerPtr mrh8_bank18 = new ReadHandlerPtr() {
        public int handler(int offset) {
            return cpu_bankbase[18].read(offset);
        }
    };
    public static ReadHandlerPtr mrh8_bank19 = new ReadHandlerPtr() {
        public int handler(int offset) {
            return cpu_bankbase[19].read(offset);
        }
    };
    public static ReadHandlerPtr mrh8_bank20 = new ReadHandlerPtr() {
        public int handler(int offset) {
            return cpu_bankbase[20].read(offset);
        }
    };
    public static ReadHandlerPtr mrh8_bank21 = new ReadHandlerPtr() {
        public int handler(int offset) {
            return cpu_bankbase[21].read(offset);
        }
    };
    public static ReadHandlerPtr mrh8_bank22 = new ReadHandlerPtr() {
        public int handler(int offset) {
            return cpu_bankbase[22].read(offset);
        }
    };
    public static ReadHandlerPtr mrh8_bank23 = new ReadHandlerPtr() {
        public int handler(int offset) {
            return cpu_bankbase[23].read(offset);
        }
    };
    public static ReadHandlerPtr mrh8_bank24 = new ReadHandlerPtr() {
        public int handler(int offset) {
            return cpu_bankbase[24].read(offset);
        }
    };

    public static WriteHandlerPtr mwh8_bank1 = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            cpu_bankbase[1].write(offset, data);
        }
    };
    public static WriteHandlerPtr mwh8_bank2 = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            cpu_bankbase[2].write(offset, data);
        }
    };
    public static WriteHandlerPtr mwh8_bank3 = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            cpu_bankbase[3].write(offset, data);
        }
    };
    public static WriteHandlerPtr mwh8_bank4 = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            cpu_bankbase[4].write(offset, data);
        }
    };
    public static WriteHandlerPtr mwh8_bank5 = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            cpu_bankbase[5].write(offset, data);
        }
    };
    public static WriteHandlerPtr mwh8_bank6 = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            cpu_bankbase[6].write(offset, data);
        }
    };
    public static WriteHandlerPtr mwh8_bank7 = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            cpu_bankbase[7].write(offset, data);
        }
    };
    public static WriteHandlerPtr mwh8_bank8 = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            cpu_bankbase[8].write(offset, data);
        }
    };
    public static WriteHandlerPtr mwh8_bank9 = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            cpu_bankbase[9].write(offset, data);
        }
    };
    public static WriteHandlerPtr mwh8_bank10 = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            cpu_bankbase[10].write(offset, data);
        }
    };
    public static WriteHandlerPtr mwh8_bank11 = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            cpu_bankbase[11].write(offset, data);
        }
    };
    public static WriteHandlerPtr mwh8_bank12 = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            cpu_bankbase[12].write(offset, data);
        }
    };
    public static WriteHandlerPtr mwh8_bank13 = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            cpu_bankbase[13].write(offset, data);
        }
    };
    public static WriteHandlerPtr mwh8_bank14 = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            cpu_bankbase[14].write(offset, data);
        }
    };
    public static WriteHandlerPtr mwh8_bank15 = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            cpu_bankbase[15].write(offset, data);
        }
    };
    public static WriteHandlerPtr mwh8_bank16 = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            cpu_bankbase[16].write(offset, data);
        }
    };
    public static WriteHandlerPtr mwh8_bank17 = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            cpu_bankbase[17].write(offset, data);
        }
    };
    public static WriteHandlerPtr mwh8_bank18 = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            cpu_bankbase[18].write(offset, data);
        }
    };
    public static WriteHandlerPtr mwh8_bank19 = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            cpu_bankbase[19].write(offset, data);
        }
    };
    public static WriteHandlerPtr mwh8_bank20 = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            cpu_bankbase[20].write(offset, data);
        }
    };
    public static WriteHandlerPtr mwh8_bank21 = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            cpu_bankbase[21].write(offset, data);
        }
    };
    public static WriteHandlerPtr mwh8_bank22 = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            cpu_bankbase[22].write(offset, data);
        }
    };
    public static WriteHandlerPtr mwh8_bank23 = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            cpu_bankbase[23].write(offset, data);
        }
    };
    public static WriteHandlerPtr mwh8_bank24 = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            cpu_bankbase[24].write(offset, data);
        }
    };

    /*-------------------------------------------------
    	init_static - sets up the static memory
    	handlers
    -------------------------------------------------*/
    static int init_static() {
        /*TODO*///	memset(rmemhandler8,  0, sizeof(rmemhandler8));
        /*TODO*///	memset(rmemhandler8s, 0, sizeof(rmemhandler8s));
        /*TODO*///	memset(rmemhandler16, 0, sizeof(rmemhandler16));
        /*TODO*///	memset(rmemhandler32, 0, sizeof(rmemhandler32));
        /*TODO*///	memset(wmemhandler8,  0, sizeof(wmemhandler8));
        /*TODO*///	memset(wmemhandler8s, 0, sizeof(wmemhandler8s));
        /*TODO*///	memset(wmemhandler16, 0, sizeof(wmemhandler16));
        /*TODO*///	memset(wmemhandler32, 0, sizeof(wmemhandler32));
        /*TODO*///
        /*TODO*///	memset(rporthandler8,  0, sizeof(rporthandler8));
        /*TODO*///	memset(rporthandler16, 0, sizeof(rporthandler16));
        /*TODO*///	memset(rporthandler32, 0, sizeof(rporthandler32));
        /*TODO*///	memset(wporthandler8,  0, sizeof(wporthandler8));
        /*TODO*///	memset(wporthandler16, 0, sizeof(wporthandler16));
        /*TODO*///	memset(wporthandler32, 0, sizeof(wporthandler32));
        /*TODO*///
        set_static_handler(STATIC_BANK1, mrh8_bank1, mwh8_bank1);/*TODO*///	set_static_handler(STATIC_BANK1,  mrh8_bank1,  NULL,         NULL,         mwh8_bank1,  NULL,         NULL);
        set_static_handler(STATIC_BANK2, mrh8_bank2, mwh8_bank2);/*TODO*///	set_static_handler(STATIC_BANK2,  mrh8_bank2,  NULL,         NULL,         mwh8_bank2,  NULL,         NULL);
        set_static_handler(STATIC_BANK3, mrh8_bank3, mwh8_bank3);/*TODO*///	set_static_handler(STATIC_BANK3,  mrh8_bank3,  NULL,         NULL,         mwh8_bank3,  NULL,         NULL);
        set_static_handler(STATIC_BANK4, mrh8_bank4, mwh8_bank4);/*TODO*///	set_static_handler(STATIC_BANK4,  mrh8_bank4,  NULL,         NULL,         mwh8_bank4,  NULL,         NULL);
        set_static_handler(STATIC_BANK5, mrh8_bank5, mwh8_bank5);/*TODO*///	set_static_handler(STATIC_BANK5,  mrh8_bank5,  NULL,         NULL,         mwh8_bank5,  NULL,         NULL);
        set_static_handler(STATIC_BANK6, mrh8_bank6, mwh8_bank6);/*TODO*///	set_static_handler(STATIC_BANK6,  mrh8_bank6,  NULL,         NULL,         mwh8_bank6,  NULL,         NULL);
        set_static_handler(STATIC_BANK7, mrh8_bank7, mwh8_bank7);/*TODO*///	set_static_handler(STATIC_BANK7,  mrh8_bank7,  NULL,         NULL,         mwh8_bank7,  NULL,         NULL);
        set_static_handler(STATIC_BANK8, mrh8_bank8, mwh8_bank8);/*TODO*///	set_static_handler(STATIC_BANK8,  mrh8_bank8,  NULL,         NULL,         mwh8_bank8,  NULL,         NULL);
        set_static_handler(STATIC_BANK9, mrh8_bank9, mwh8_bank9);/*TODO*///	set_static_handler(STATIC_BANK9,  mrh8_bank9,  NULL,         NULL,         mwh8_bank9,  NULL,         NULL);
        set_static_handler(STATIC_BANK10, mrh8_bank10, mwh8_bank10);/*TODO*///	set_static_handler(STATIC_BANK10, mrh8_bank10, NULL,         NULL,         mwh8_bank10, NULL,         NULL);
        set_static_handler(STATIC_BANK11, mrh8_bank11, mwh8_bank11);/*TODO*///	set_static_handler(STATIC_BANK11, mrh8_bank11, NULL,         NULL,         mwh8_bank11, NULL,         NULL);
        set_static_handler(STATIC_BANK12, mrh8_bank12, mwh8_bank12);/*TODO*///	set_static_handler(STATIC_BANK12, mrh8_bank12, NULL,         NULL,         mwh8_bank12, NULL,         NULL);
        set_static_handler(STATIC_BANK13, mrh8_bank13, mwh8_bank13);/*TODO*///	set_static_handler(STATIC_BANK13, mrh8_bank13, NULL,         NULL,         mwh8_bank13, NULL,         NULL);
        set_static_handler(STATIC_BANK14, mrh8_bank14, mwh8_bank14);/*TODO*///	set_static_handler(STATIC_BANK14, mrh8_bank14, NULL,         NULL,         mwh8_bank14, NULL,         NULL);
        set_static_handler(STATIC_BANK15, mrh8_bank15, mwh8_bank15);/*TODO*///	set_static_handler(STATIC_BANK15, mrh8_bank15, NULL,         NULL,         mwh8_bank15, NULL,         NULL);
        set_static_handler(STATIC_BANK16, mrh8_bank16, mwh8_bank16);/*TODO*///	set_static_handler(STATIC_BANK16, mrh8_bank16, NULL,         NULL,         mwh8_bank16, NULL,         NULL);
        set_static_handler(STATIC_BANK17, mrh8_bank17, mwh8_bank17);/*TODO*///	set_static_handler(STATIC_BANK17, mrh8_bank17, NULL,         NULL,         mwh8_bank17, NULL,         NULL);
        set_static_handler(STATIC_BANK18, mrh8_bank18, mwh8_bank18);/*TODO*///	set_static_handler(STATIC_BANK18, mrh8_bank18, NULL,         NULL,         mwh8_bank18, NULL,         NULL);
        set_static_handler(STATIC_BANK19, mrh8_bank19, mwh8_bank19);/*TODO*///	set_static_handler(STATIC_BANK19, mrh8_bank19, NULL,         NULL,         mwh8_bank19, NULL,         NULL);
        set_static_handler(STATIC_BANK20, mrh8_bank20, mwh8_bank20);/*TODO*///	set_static_handler(STATIC_BANK20, mrh8_bank20, NULL,         NULL,         mwh8_bank20, NULL,         NULL);
        set_static_handler(STATIC_BANK21, mrh8_bank21, mwh8_bank21);/*TODO*///	set_static_handler(STATIC_BANK21, mrh8_bank21, NULL,         NULL,         mwh8_bank21, NULL,         NULL);
        set_static_handler(STATIC_BANK22, mrh8_bank22, mwh8_bank22);/*TODO*///	set_static_handler(STATIC_BANK22, mrh8_bank22, NULL,         NULL,         mwh8_bank22, NULL,         NULL);
        set_static_handler(STATIC_BANK23, mrh8_bank23, mwh8_bank23);/*TODO*///	set_static_handler(STATIC_BANK23, mrh8_bank23, NULL,         NULL,         mwh8_bank23, NULL,         NULL);
        set_static_handler(STATIC_BANK24, mrh8_bank24, mwh8_bank24);/*TODO*///	set_static_handler(STATIC_BANK24, mrh8_bank24, NULL,         NULL,         mwh8_bank24, NULL,         NULL);
        set_static_handler(STATIC_UNMAP, mrh8_bad, mwh8_bad);/*TODO*///	set_static_handler(STATIC_UNMAP,  mrh8_bad,    mrh16_bad,    mrh32_bad,    mwh8_bad,    mwh16_bad,    mwh32_bad);
        set_static_handler(STATIC_NOP, mrh8_nop, mwh8_nop);/*TODO*///	set_static_handler(STATIC_NOP,    mrh8_nop,    mrh16_nop,    mrh32_nop,    mwh8_nop,    mwh16_nop,    mwh32_nop);
        set_static_handler(STATIC_RAM, mrh8_ram, mwh8_ram);/*TODO*///	set_static_handler(STATIC_RAM,    mrh8_ram,    NULL,         NULL,         mwh8_ram,    NULL,         NULL);
        set_static_handler(STATIC_ROM, null, mwh8_rom);/*TODO*///	set_static_handler(STATIC_ROM,    NULL,        NULL,         NULL,         mwh8_rom,    mwh16_rom,    mwh32_rom);
        set_static_handler(STATIC_RAMROM, null, mwh8_ramrom);/*TODO*///	set_static_handler(STATIC_RAMROM, NULL,        NULL,         NULL,         mwh8_ramrom, mwh16_ramrom, mwh32_ramrom);

        /* override port unmapped handlers */
        rporthandler8[STATIC_UNMAP].handler = prh8_bad;
        /*TODO*///	rporthandler16[STATIC_UNMAP].handler = (void *)prh16_bad;
        /*TODO*///	rporthandler32[STATIC_UNMAP].handler = (void *)prh32_bad;
        wporthandler8[STATIC_UNMAP].handler = pwh8_bad;
        /*TODO*///	wporthandler16[STATIC_UNMAP].handler = (void *)pwh16_bad;
        /*TODO*///	wporthandler32[STATIC_UNMAP].handler = (void *)pwh32_bad;
        /*TODO*///
        return 1;
    }

    /*-------------------------------------------------
    	debugging
    -------------------------------------------------*/
    static void dump_map(FILE file, memport_data memport, table_data table) {
        String strings[]
                = {
                    "invalid", "bank 1", "bank 2", "bank 3",
                    "bank 4", "bank 5", "bank 6", "bank 7",
                    "bank 8", "bank 9", "bank 10", "bank 11",
                    "bank 12", "bank 13", "bank 14", "bank 15",
                    "bank 16", "bank 17", "bank 18", "bank 19",
                    "bank 20", "bank 21", "bank 22", "bank 23",
                    "bank 24", "RAM", "ROM", "RAMROM",
                    "nop", "unused 1", "unused 2", "unmapped"
                };

        int minbits = DATABITS_TO_SHIFT(memport.dbits);
        int l1bits = LEVEL1_BITS(memport.ebits);
        int l2bits = LEVEL2_BITS(memport.ebits);
        int l1count = 1 << l1bits;
        int l2count = 1 << l2bits;
        int i, j;

        fprintf(file, "  Address bits = %d\n", memport.abits);
        fprintf(file, "     Data bits = %d\n", memport.dbits);
        fprintf(file, "Effective bits = %d\n", memport.ebits);
        fprintf(file, "       L1 bits = %d\n", l1bits);
        fprintf(file, "       L2 bits = %d\n", l2bits);
        fprintf(file, "  Address mask = %X\n", memport.mask);
        fprintf(file, "\n");

        for (i = 0; i < l1count; i++) {
            char entry = table.table.read(i);
            if (entry != STATIC_UNMAP) {
                fprintf(file, "%05X  %08X-%08X    = %02X: ", i,
                        i << (l2bits + minbits),
                        ((i + 1) << (l2bits + minbits)) - 1, (int) entry);
                if (entry < STATIC_COUNT) {
                    fprintf(file, "%s [offset=%08X]\n", strings[entry], table.handlers[entry].offset);
                } else if (entry < SUBTABLE_BASE) {
                    fprintf(file, "handler(%08X) [offset=%08X]\n", table.handlers[entry].handler.hashCode(), table.handlers[entry].offset);
                } else {
                    fprintf(file, "subtable %d\n", entry & SUBTABLE_MASK);
                    entry &= SUBTABLE_MASK;

                    for (j = 0; j < l2count; j++) {
                        char/*UINT8*/ entry2 = table.table.read((1 << l1bits) + (entry << l2bits) + j);
                        if (entry2 != STATIC_UNMAP) {
                            fprintf(file, "   %05X  %08X-%08X = %02X: ", j,
                                    (i << (l2bits + minbits)) | (j << minbits),
                                    ((i << (l2bits + minbits)) | ((j + 1) << minbits)) - 1, (int) entry2);
                            if (entry2 < STATIC_COUNT) {
                                fprintf(file, "%s [offset=%08X]\n", strings[entry2], table.handlers[entry2].offset);
                            } else if (entry2 < SUBTABLE_BASE) {
                                fprintf(file, "handler(%08X) [offset=%08X]\n", table.handlers[entry2].handler.hashCode(), table.handlers[entry2].offset);
                            } else {
                                fprintf(file, "subtable %d???????????\n", entry2 & SUBTABLE_MASK);
                            }
                        }
                    }
                }
            }
        }
    }

    public static void mem_dump() {
        FILE file = fopen("memdump.log", "w");
        int cpunum;

        /* skip if we can't open the file */
        if (file == null) {
            return;
        }

        /* loop over CPUs */
        for (cpunum
                = 0; cpunum
                < cpu_gettotalcpu(); cpunum++) {
            /* memory handlers */
            if (cpudata[cpunum].mem.abits != 0) {
                fprintf(file, "\n\n"
                        + "===============================\n"
                        + "CPU %d read memory handler dump\n"
                        + "===============================\n", cpunum);
                dump_map(file, cpudata[cpunum].mem, cpudata[cpunum].mem.read);

                fprintf(file, "\n\n"
                        + "================================\n"
                        + "CPU %d write memory handler dump\n"
                        + "================================\n", cpunum);
                dump_map(file, cpudata[cpunum].mem, cpudata[cpunum].mem.write);
            }

            /* port handlers */
            if (cpudata[cpunum].port.abits != 0) {
                fprintf(file, "\n\n"
                        + "=============================\n"
                        + "CPU %d read port handler dump\n"
                        + "=============================\n", cpunum);
                dump_map(file, cpudata[cpunum].port, cpudata[cpunum].port.read);

                fprintf(file, "\n\n"
                        + "==============================\n"
                        + "CPU %d write port handler dump\n"
                        + "==============================\n", cpunum);
                dump_map(file, cpudata[cpunum].port, cpudata[cpunum].port.write);
            }
        }
        fclose(file);
    }
}
