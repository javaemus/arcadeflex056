/*
 * ported to v0.37b5
 *
 */
package mame037b5.sound;

import static arcadeflex056.fucPtr.*;
import static common.ptr.*;
import static common.libc.cstdlib.*;
import static mame056.timer.*;
import static mame056.sound.streams.*;

public class ym2151 {
    public static class OscilRec {
        int phase;		/*accumulated operator phase*/
        int freq;		/*operator frequency*/
        int DT1v;	/*operator DT1 phase inc/decrement*/

        int MUL;		/*phase multiply*/
        int DT1;		/*DT1 * 32      */
        int DT2;		/*DT2 index     */

        int[] connect;	/*operator output 'direction'*/
        /*Begin of channel specific data*/
        /*note: each operator number 0 contains channel specific data*/
        int FeedBack;	/*feedback shift value for operators 0 in each channel*/
        int FB;		/*operator self feedback value used only by operators 0*/
        int FB0;		/*previous output value*/
        int KC;		/*operator KC (copied to all operators)*/
        int KCindex;	/*speedup*/
        int PMS;		/*channel PMS*/
        int AMS;		/*channel AMS*/
        /*End of channel specific data*/

        int AMSmask;	/*LFO AMS enable mask*/

        int state;		/*Envelope state: 4-attack(AR) 3-decay(D1R) 2-sustain(D2R) 1-release(RR) 0-off*/
        int delta_AR;	/*volume delta for attack phase*/
        int TL;		/*Total attenuation Level*/
        int volume;	/*operator attenuation level*/
        int delta_D1R;	/*volume delta for decay phase*/
        int D1L;		/*EG switches to D2R, when envelope reaches this level*/
        int delta_D2R;	/*volume delta for sustain phase*/
        int delta_RR;	/*volume delta for release phase*/

        int key;		/*0=last key was KEY OFF, 1=last key was KEY ON*/

        int KS;		/*Key Scale     */
        int AR;		/*Attack rate   */
        int D1R;		/*Decay rate    */
        int D2R;		/*Sustain rate  */
        int RR;		/*Release rate  */

        /*unsigned*/ int reserved0;	/**/
        int reserved1;	/**/
        int reserved2;	/**/

    }

    public static class _YM2151 {
        public _YM2151() {
            Oscils = new OscilRec[32];
            PAN = new int[16];
            TimerATime = new double[1024];
            TimerBTime = new double[256];
            freq = new int[11 * 12 * 64];
            DT1freq = new int[8 * 16 * 32];
            EG_tab = new int[32 + 64 + 32];
            LFOfreq = new int[256];
            noise_tab = new int[32];
        }

        OscilRec[] Oscils;	/*there are 32 operators in YM2151*/

        int[] PAN;	/*channels output masks (0xffffffff = enable)*/

        int LFOphase;	/*accumulated LFO phase         */
        int LFOfrq;	/*LFO frequency                 */
        int LFOwave;	/*LFO waveform (0-saw, 1-square, 2-triangle, 3-random noise)*/
        int PMD;		/*LFO Phase Modulation Depth    */
        int AMD;		/*LFO Amplitude Modulation Depth*/
        int LFA;		/*current AM from LFO*/
        int LFP;		/*current PM from LFO*/

        int test;		/*TEST register*/

        int CT;		/*output control pins (bit7 CT2, bit6 CT1)*/
        int noise;		/*noise register (bit 7 - noise enable, bits 4-0 - noise period*/
        int noiseRNG;	/*17 bit noise shift register*/
        int noise_p;	/*noise 'phase'*/
        int noise_f;	/*noise period*/

        int CSMreq;	/*CSM KEYON/KEYOFF sequence request*/

        int IRQenable;	/*IRQ enable for timer B (bit 3) and timer A (bit 2); bit 7 - CSM mode (keyon to all slots, everytime timer A overflows)*/
        int status;	/*chip status (BUSY, IRQ Flags)*/


        Object TimATimer, TimBTimer;	/*ASG 980324 -- added for tracking timers*/
        double[] TimerATime;	/*Timer A times for MAME*/
        double[] TimerBTime;		/*Timer B times for MAME*/

        int TimAIndex;		/*Timer A index*/
        int TimBIndex;		/*Timer B index*/

        int TimAOldIndex;	/*Timer A previous index*/
        int TimBOldIndex;	/*Timer B previous index*/

        /*
    *   Frequency-deltas to get the closest frequency possible.
	*   There're 11 octaves because of DT2 (max 950 cents over base frequency)
	*   and LFO phase modulation (max 800 cents below AND over base frequency)
	*   Summary:   octave  explanation
	*              0       note code - LFO PM
	*              1       note code
	*              2       note code
	*              3       note code
	*              4       note code
	*              5       note code
	*              6       note code
	*              7       note code
	*              8       note code
	*              9       note code + DT2 + LFO PM
	*              10      note code + DT2 + LFO PM
	*/
        int freq[];/*11 octaves, 768 'cents' per octave*/

        /*
    	*   Frequency deltas for DT1. These deltas alter operator frequency
    	*   after it has been taken from frequency-deltas table.
    	*/
        int[] DT1freq;		/*8 DT1 levels, 32 KC values*/
        int[] EG_tab;	/*Envelope Generator deltas (32 + 64 rates + 32 RKS)*/
        int[] LFOfreq;		/*LFO frequency deltas*/
        int[] noise_tab;		/*17bit Noise Generator periods*/

        WriteYmHandlerPtr irqhandler;				/*IRQ function handler*/
        WriteHandlerPtr porthandler;	/*port write function handler*/

        /*unsigned*/ int clock;			/*chip clock in Hz (passed from 2151intf.c)*/
        /*unsigned*/ int sampfreq;		/*sampling frequency in Hz (passed from 2151intf.c)*/

    }

    /*
    **  Shifts below are subject to change when sampling frequency changes...
    */
    public static final int FREQ_SH = 16;  /* 16.16 fixed point (frequency calculations) */
    public static final int ENV_SH = 16;  /* 16.16 fixed point (envelope calculations)  */
    public static final int LFO_SH = 23;  /*  9.23 fixed point (LFO calculations)       */
    public static final int TIMER_SH = 16;  /* 16.16 fixed point (timers calculations)    */

    public static final int FREQ_MASK = ((1 << FREQ_SH) - 1);
    public static final int ENV_MASK = ((1 << ENV_SH) - 1);

    public static final int ENV_BITS = 10;
    public static final int ENV_LEN = (1 << ENV_BITS);
    public static final double ENV_STEP = (128.0 / ENV_LEN);
    public static final int ENV_QUIET = ((int) (0x68 / (ENV_STEP)));

    public static final int MAX_ATT_INDEX = ((ENV_LEN << ENV_SH) - 1); /*1023.ffff*/
    public static final int MIN_ATT_INDEX = ((1 << ENV_SH) - 1); /*   0.ffff*/

    public static final int EG_ATT = 4;
    public static final int EG_DEC = 3;
    public static final int EG_SUS = 2;
    public static final int EG_REL = 1;
    public static final int EG_OFF = 0;

    public static final int SIN_BITS = 10;
    public static final int SIN_LEN = (1 << SIN_BITS);
    public static final int SIN_MASK = (SIN_LEN - 1);

    public static final int TL_RES_LEN = (256); /* 8 bits addressing (real chip) */

    public static final int LFO_BITS = 9;
    public static final int LFO_LEN = (1 << LFO_BITS);
    public static final int LFO_MASK = (LFO_LEN - 1);

    public static final int FINAL_SH = (0);
    public static final int MAXOUT = (+32767);
    public static final int MINOUT = (-32768);


    /* TL_TAB_LEN is calculated as:
     * 13 - sinus amplitude bits  (Y axis)
     * 2  - sinus sign bit        (Y axis)
     * ENV_LEN - sinus resolution (X axis)
    */
    public static final int TL_TAB_LEN = (13 * 2 * TL_RES_LEN);
    static int[] TL_TAB = new int[TL_TAB_LEN];

    /* sin waveform table in 'decibel' scale*/
    static int[] sin_tab = new int[SIN_LEN];

    /* four AM/PM LFO waveforms (8 in total)*/
    static int[] lfo_tab = new int[LFO_LEN * 4 * 2];

    /* LFO amplitude modulation depth table (128 levels)*/
    static int[] lfo_md_tab = new int[128];

    /* translate from D1L to volume index (16 D1L levels)*/
    static int[] D1L_tab = new int[16];

    /*
     *   DT2 defines offset in cents from base note
     *
     *   This table defines offset in frequency-deltas table.
     *   User's Manual page 22
     *
     *   Values below were calculated using formula: value =  orig.val / 1.5625
     *
     *	DT2=0 DT2=1 DT2=2 DT2=3
     *	0     600   781   950
    */
    static int DT2_tab[] = {0, 384, 500, 608};

    /*
     *   DT1 defines offset in Hertz from base note
     *   This table is converted while initialization...
     *   Detune table in YM2151 User's Manual is wrong (checked against the real chip)
    */
    static int DT1_tab[] = { /* 4*32 DT1 values */
    /* DT1=0 */
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,

    /* DT1=1 */
            0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2,
            2, 3, 3, 3, 4, 4, 4, 5, 5, 6, 6, 7, 8, 8, 8, 8,

    /* DT1=2 */
            1, 1, 1, 1, 2, 2, 2, 2, 2, 3, 3, 3, 4, 4, 4, 5,
            5, 6, 6, 7, 8, 8, 9, 10, 11, 12, 13, 14, 16, 16, 16, 16,

    /* DT1=3 */
            2, 2, 2, 2, 2, 3, 3, 3, 4, 4, 4, 5, 5, 6, 6, 7,
            8, 8, 9, 10, 11, 12, 13, 14, 16, 17, 19, 20, 22, 22, 22, 22
    };
    static int phaseinc_rom[] = {
            1299, 1300, 1301, 1302, 1303, 1304, 1305, 1306, 1308, 1309, 1310, 1311, 1313, 1314, 1315, 1316,
            1318, 1319, 1320, 1321, 1322, 1323, 1324, 1325, 1327, 1328, 1329, 1330, 1332, 1333, 1334, 1335,
            1337, 1338, 1339, 1340, 1341, 1342, 1343, 1344, 1346, 1347, 1348, 1349, 1351, 1352, 1353, 1354,
            1356, 1357, 1358, 1359, 1361, 1362, 1363, 1364, 1366, 1367, 1368, 1369, 1371, 1372, 1373, 1374,
            1376, 1377, 1378, 1379, 1381, 1382, 1383, 1384, 1386, 1387, 1388, 1389, 1391, 1392, 1393, 1394,
            1396, 1397, 1398, 1399, 1401, 1402, 1403, 1404, 1406, 1407, 1408, 1409, 1411, 1412, 1413, 1414,
            1416, 1417, 1418, 1419, 1421, 1422, 1423, 1424, 1426, 1427, 1429, 1430, 1431, 1432, 1434, 1435,
            1437, 1438, 1439, 1440, 1442, 1443, 1444, 1445, 1447, 1448, 1449, 1450, 1452, 1453, 1454, 1455,
            1458, 1459, 1460, 1461, 1463, 1464, 1465, 1466, 1468, 1469, 1471, 1472, 1473, 1474, 1476, 1477,
            1479, 1480, 1481, 1482, 1484, 1485, 1486, 1487, 1489, 1490, 1492, 1493, 1494, 1495, 1497, 1498,
            1501, 1502, 1503, 1504, 1506, 1507, 1509, 1510, 1512, 1513, 1514, 1515, 1517, 1518, 1520, 1521,
            1523, 1524, 1525, 1526, 1528, 1529, 1531, 1532, 1534, 1535, 1536, 1537, 1539, 1540, 1542, 1543,
            1545, 1546, 1547, 1548, 1550, 1551, 1553, 1554, 1556, 1557, 1558, 1559, 1561, 1562, 1564, 1565,
            1567, 1568, 1569, 1570, 1572, 1573, 1575, 1576, 1578, 1579, 1580, 1581, 1583, 1584, 1586, 1587,
            1590, 1591, 1592, 1593, 1595, 1596, 1598, 1599, 1601, 1602, 1604, 1605, 1607, 1608, 1609, 1610,
            1613, 1614, 1615, 1616, 1618, 1619, 1621, 1622, 1624, 1625, 1627, 1628, 1630, 1631, 1632, 1633,
            1637, 1638, 1639, 1640, 1642, 1643, 1645, 1646, 1648, 1649, 1651, 1652, 1654, 1655, 1656, 1657,
            1660, 1661, 1663, 1664, 1666, 1667, 1669, 1670, 1672, 1673, 1675, 1676, 1678, 1679, 1681, 1682,
            1685, 1686, 1688, 1689, 1691, 1692, 1694, 1695, 1697, 1698, 1700, 1701, 1703, 1704, 1706, 1707,
            1709, 1710, 1712, 1713, 1715, 1716, 1718, 1719, 1721, 1722, 1724, 1725, 1727, 1728, 1730, 1731,
            1734, 1735, 1737, 1738, 1740, 1741, 1743, 1744, 1746, 1748, 1749, 1751, 1752, 1754, 1755, 1757,
            1759, 1760, 1762, 1763, 1765, 1766, 1768, 1769, 1771, 1773, 1774, 1776, 1777, 1779, 1780, 1782,
            1785, 1786, 1788, 1789, 1791, 1793, 1794, 1796, 1798, 1799, 1801, 1802, 1804, 1806, 1807, 1809,
            1811, 1812, 1814, 1815, 1817, 1819, 1820, 1822, 1824, 1825, 1827, 1828, 1830, 1832, 1833, 1835,
            1837, 1838, 1840, 1841, 1843, 1845, 1846, 1848, 1850, 1851, 1853, 1854, 1856, 1858, 1859, 1861,
            1864, 1865, 1867, 1868, 1870, 1872, 1873, 1875, 1877, 1879, 1880, 1882, 1884, 1885, 1887, 1888,
            1891, 1892, 1894, 1895, 1897, 1899, 1900, 1902, 1904, 1906, 1907, 1909, 1911, 1912, 1914, 1915,
            1918, 1919, 1921, 1923, 1925, 1926, 1928, 1930, 1932, 1933, 1935, 1937, 1939, 1940, 1942, 1944,
            1946, 1947, 1949, 1951, 1953, 1954, 1956, 1958, 1960, 1961, 1963, 1965, 1967, 1968, 1970, 1972,
            1975, 1976, 1978, 1980, 1982, 1983, 1985, 1987, 1989, 1990, 1992, 1994, 1996, 1997, 1999, 2001,
            2003, 2004, 2006, 2008, 2010, 2011, 2013, 2015, 2017, 2019, 2021, 2022, 2024, 2026, 2028, 2029,
            2032, 2033, 2035, 2037, 2039, 2041, 2043, 2044, 2047, 2048, 2050, 2052, 2054, 2056, 2058, 2059,
            2062, 2063, 2065, 2067, 2069, 2071, 2073, 2074, 2077, 2078, 2080, 2082, 2084, 2086, 2088, 2089,
            2092, 2093, 2095, 2097, 2099, 2101, 2103, 2104, 2107, 2108, 2110, 2112, 2114, 2116, 2118, 2119,
            2122, 2123, 2125, 2127, 2129, 2131, 2133, 2134, 2137, 2139, 2141, 2142, 2145, 2146, 2148, 2150,
            2153, 2154, 2156, 2158, 2160, 2162, 2164, 2165, 2168, 2170, 2172, 2173, 2176, 2177, 2179, 2181,
            2185, 2186, 2188, 2190, 2192, 2194, 2196, 2197, 2200, 2202, 2204, 2205, 2208, 2209, 2211, 2213,
            2216, 2218, 2220, 2222, 2223, 2226, 2227, 2230, 2232, 2234, 2236, 2238, 2239, 2242, 2243, 2246,
            2249, 2251, 2253, 2255, 2256, 2259, 2260, 2263, 2265, 2267, 2269, 2271, 2272, 2275, 2276, 2279,
            2281, 2283, 2285, 2287, 2288, 2291, 2292, 2295, 2297, 2299, 2301, 2303, 2304, 2307, 2308, 2311,
            2315, 2317, 2319, 2321, 2322, 2325, 2326, 2329, 2331, 2333, 2335, 2337, 2338, 2341, 2342, 2345,
            2348, 2350, 2352, 2354, 2355, 2358, 2359, 2362, 2364, 2366, 2368, 2370, 2371, 2374, 2375, 2378,
            2382, 2384, 2386, 2388, 2389, 2392, 2393, 2396, 2398, 2400, 2402, 2404, 2407, 2410, 2411, 2414,
            2417, 2419, 2421, 2423, 2424, 2427, 2428, 2431, 2433, 2435, 2437, 2439, 2442, 2445, 2446, 2449,
            2452, 2454, 2456, 2458, 2459, 2462, 2463, 2466, 2468, 2470, 2472, 2474, 2477, 2480, 2481, 2484,
            2488, 2490, 2492, 2494, 2495, 2498, 2499, 2502, 2504, 2506, 2508, 2510, 2513, 2516, 2517, 2520,
            2524, 2526, 2528, 2530, 2531, 2534, 2535, 2538, 2540, 2542, 2544, 2546, 2549, 2552, 2553, 2556,
            2561, 2563, 2565, 2567, 2568, 2571, 2572, 2575, 2577, 2579, 2581, 2583, 2586, 2589, 2590, 2593
    };

    static _YM2151[] YMPSG = null;	/* array of YM2151's */
    static int YMNumChips;	/* total # of YM2151's emulated */


    /*these variables stay here because of speedup purposes only */
    static _YM2151 PSG = null;
    static int[][] chanout = new int[8][1];
    static int[] c1 = new int[1];
    static int[] m2 = new int[1];
    static int[] c2 = new int[1]; /*Phase Modulation input for operators 2,3,4*/

    static void init_tables() {
        int i, x;
        int n;
        double o, m = 0.0;

        for (x = 0; x < TL_RES_LEN; x++) {
            m = (1 << 16) / Math.pow(2, (x + 1) * (ENV_STEP / 4.0) / 8.0);
            m = Math.floor(m);

    		/* we never reach (1<<16) here due to the (x+1) */
    		/* result fits within 16 bits at maximum */

            n = (int) m;		/* 16 bits here */
            n >>= 4;		/* 12 bits here */
            if ((n & 1) != 0)		/* round to closest */
                n = (n >> 1) + 1;
            else
                n = n >> 1;
    						/* 11 bits here (rounded) */
            n <<= 2;		/* 13 bits here (as in real chip) */
            TL_TAB[x * 2 + 0] = n;
            TL_TAB[x * 2 + 1] = -TL_TAB[x * 2 + 0];

            for (i = 1; i < 13; i++) {
                TL_TAB[x * 2 + 0 + i * 2 * TL_RES_LEN] = TL_TAB[x * 2 + 0] >> i;
                TL_TAB[x * 2 + 1 + i * 2 * TL_RES_LEN] = -TL_TAB[x * 2 + 0 + i * 2 * TL_RES_LEN];
            }
        }
    	/*logerror("TL_TAB_LEN = %i (%i bytes)\n",TL_TAB_LEN, (int)sizeof(TL_TAB));*/

        for (i = 0; i < SIN_LEN; i++) {
    		/* non-standard sinus */
            m = Math.sin(((i * 2) + 1) * Math.PI / SIN_LEN); /* checked against the real chip */

    		/* we never reach zero here due to ((i*2)+1) */

            if (m > 0.0)
                o = 8 * Math.log(1.0 / m) / Math.log(2);  /* convert to 'decibels' */
            else
                o = 8 * Math.log(-1.0 / m) / Math.log(2); /* convert to 'decibels' */

            o = o / (ENV_STEP / 4);

            n = (int) (2.0 * o);
            if ((n & 1) != 0)		/* round to closest */
                n = (n >> 1) + 1;
            else
                n = n >> 1;

            sin_tab[i] = n * 2 + (m >= 0.0 ? 0 : 1);
    		/*logerror("sin [%4i]= %4i (TL_TAB value=%5i)\n", i, sin_tab[i],TL_TAB[sin_tab[i]]);*/
        }

    	/*logerror("ENV_QUIET= %08x\n",ENV_QUIET );*/


    	/* calculate LFO AM waveforms*/
        for (x = 0; x < 4; x++) {
            for (i = 0; i < LFO_LEN; i++) {
                switch (x) {
                    case 0:	/* saw (255 down to 0) */
                        m = 255 - (i / 2);
                        break;
                    case 1: /* square (255,0) */
                        if (i < 256)
                            m = 255;
                        else
                            m = 0;
                        break;
                    case 2: /* triangle (255 down to 0, up to 255) */
                        if (i < 256)
                            m = 255 - i;
                        else
                            m = i - 256;
                        break;
                    case 3: /* random (range 0 to 255) */
                        m = ((int) rand()) & 255;
                        break;
                }
    		/* we reach m = zero here !!!*/

                if (m > 0.0)
                    o = 8 * Math.log(255.0 / m) / Math.log(2);  /* convert to 'decibels' */
                else {
                    if (m < 0.0)
                        o = 8 * Math.log(-255.0 / m) / Math.log(2); /* convert to 'decibels' */
                    else
                        o = 8 * Math.log(255.0 / 0.01) / Math.log(2); /* small number */
                }

                o = o / (ENV_STEP / 4);

                n = (int) (2.0 * o);
                if ((n & 1) != 0)		/* round to closest */
                    n = (n >> 1) + 1;
                else
                    n = n >> 1;

                lfo_tab[x * LFO_LEN * 2 + i * 2] = n * 2 + (m >= 0.0 ? 0 : 1);
    		/*if (errorlog) fprintf(errorlog,"lfo am waveofs[%i] %04i = %i\n", x, i*2, lfo_tab[ x*LFO_LEN*2 + i*2 ] );*/
            }
        }
        for (i = 0; i < 128; i++) {
            m = i * 2; /*m=0,2,4,6,8,10,..,252,254*/

    		/* we reach m = zero here !!!*/

            if (m > 0.0)
                o = 8 * Math.log(8192.0 / m) / Math.log(2);  /* convert to 'decibels' */
            else
                o = 8 * Math.log(8192.0 / 0.01) / Math.log(2); /* small number (m=0)*/

            o = o / (ENV_STEP / 4);

            n = (int) (2.0 * o);
            if ((n & 1) != 0)		/* round to closest */
                n = (n >> 1) + 1;
            else
                n = n >> 1;

            lfo_md_tab[i] = n * 2;
    		/*if (errorlog) fprintf(errorlog,"lfo_md_tab[%i](%i) = ofs %i shr by %i\n", i, i*2, (lfo_md_tab[i]>>1)&255, lfo_md_tab[i]>>9 );*/
        }

    	/* calculate LFO PM waveforms*/
        for (x = 0; x < 4; x++) {
            for (i = 0; i < LFO_LEN; i++) {
                switch (x) {
                    case 0:	/* saw (0 to 127, -128 to -1) */
                        if (i < 256)
                            m = (i / 2);
                        else
                            m = (i / 2) - 256;
                        break;
                    case 1: /* square (127,-128) */
                        if (i < 256)
                            m = 127;
                        else
                            m = -128;
                        break;
                    case 2: /* triangle (0 to 127,127 to -128,-127 to 0) */
                        if (i < 128)
                            m = i; /*0 to 127*/
                        else {
                            if (i < 384)
                                m = 255 - i; /*127 down to -128*/
                            else
                                m = i - 511; /*-127 to 0*/
                        }
                        break;
                    case 3: /* random (range -128 to 127) */
                        m = ((int) rand()) & 255;
                        m -= 128;
                        break;
                }
    		/* we reach m = zero here !!!*/

                if (m > 0.0)
                    o = 8 * Math.log(127.0 / m) / Math.log(2);  /* convert to 'decibels' */
                else {
                    if (m < 0.0)
                        o = 8 * Math.log(-128.0 / m) / Math.log(2); /* convert to 'decibels' */
                    else
                        o = 8 * Math.log(127.0 / 0.01) / Math.log(2); /* small number */
                }

                o = o / (ENV_STEP / 4);

                n = (int) (2.0 * o);
                if ((n & 1) != 0)		/* round to closest */
                    n = (n >> 1) + 1;
                else
                    n = n >> 1;

                lfo_tab[x * LFO_LEN * 2 + i * 2 + 1] = n * 2 + (m >= 0.0 ? 0 : 1);
    		/*if (errorlog) fprintf(errorlog,"lfo pm waveofs[%i] %04i = %i\n", x, i*2+1, lfo_tab[ x*LFO_LEN*2 + i*2 + 1 ] );*/
            }
        }


    	/* calculate D1L_tab table */
        for (i = 0; i < 16; i++) {
            m = (i != 15 ? i : i + 16) * (4.0 / ENV_STEP);   /*every 3 'dB' except for all bits = 1 = 45dB+48dB*/
            D1L_tab[i] = (int) (m * (1 << ENV_SH));
		/*logerror("D1L_tab[%02x]=%08x\n",i,D1L_tab[i] );*/

            m = (i < 15 ? i : i + 16) * (4.0 / ENV_STEP);   /*every 3 'dB' except for all bits = 1 = 45dB+48dB*/
            D1L_tab[i] = (int) (m * (1 << ENV_SH));
    		/*if (errorlog) fprintf(errorlog,"D1L_tab[%04x]=%08x\n",i,D1L_tab[i] );*/
        }

    }

    static void init_chip_tables(_YM2151 chip) {
        int i, j;
        double mult, pom, pom2, clk, phaseinc, Hz;

        double scaler;	/* formula below is true for chip clock=3579545 */
    	/* so we need to scale its output accordingly to the chip clock */

        scaler = ((double) chip.clock / 64.0) / ((double) chip.sampfreq);

    	/*this loop calculates Hertz values for notes from c-0 to b-7*/
    	/*including 64 'cents' (100/64 that is 1.5625 of real cent) per note*/
    	/* i*100/64/1200 is equal to i/768 */

        mult = (1 << (FREQ_SH - 10)); /* -10 because phaseinc_rom table values are already in 10.10 format */
        for (i = 0; i < 768; i++) {
		/* 3.4375 Hz is note A; C# is 4 semitones higher */
            Hz = 1000;
            phaseinc = phaseinc_rom[i];	/*real chip phase increment*/
            phaseinc *= scaler;			/*adjust*/


		/*octave 2 - reference octave*/
            chip.freq[768 + 2 * 768 + i] = ((int) (phaseinc * mult)) & 0xffffffc0; /*adjust to X.10 fixed point*/

		/*octave 0 and octave 1*/
            for (j = 0; j < 2; j++) {
                chip.freq[768 + j * 768 + i] = (chip.freq[768 + 2 * 768 + i] >> (2 - j)) & 0xffffffc0; /*adjust to X.10 fixed point*/
            }

		/*octave 3 to 7*/
            for (j = 3; j < 8; j++) {
                chip.freq[768 + j * 768 + i] = chip.freq[768 + 2 * 768 + i] << (j - 2);
            }
        }
			/*octave -1 (all equal to: oct 0, KC 00, KF 00) */
        for (i = 0; i < 768; i++) {
            chip.freq[0 * 768 + i] = chip.freq[1 * 768 + 0];
        }

	/*octave 8 and 9 (all equal to: oct 7, _KC_14_, _KF_63_) */
        for (j = 8; j < 10; j++) {
            for (i = 0; i < 768; i++) {
                chip.freq[768 + j * 768 + i] = chip.freq[768 + 8 * 768 - 1];
            }
        }


        mult = (1 << FREQ_SH);
        for (j = 0; j < 4; j++) {
            for (i = 0; i < 32; i++) {
                Hz = ((double) DT1_tab[j * 32 + i] * ((double) chip.clock / 64.0)) / (double) (1 << 20);

			/*calculate phase increment*/
                phaseinc = (Hz * SIN_LEN) / (double) chip.sampfreq;

			/*positive and negative values*/
                chip.DT1freq[(j + 0) * 32 + i] = (int) (phaseinc * mult);
                chip.DT1freq[(j + 4) * 32 + i] = -chip.DT1freq[(j + 0) * 32 + i];
            }
        }
        mult = (1 << LFO_SH);
        clk = (double) chip.clock;
        for (i = 0; i < 256; i++) {
            j = i & 0x0f;
            pom = Math.abs((clk / 65536 / (1 << (i / 16))) - (clk / 65536 / 32 / (1 << (i / 16)) * (j + 1)));

		/*calculate phase increment*/
            chip.LFOfreq[0xff - i] = (int) (((pom * LFO_LEN) / (double) chip.sampfreq) * mult); /*fixed point*/
		/*logerror("LFO[%02x] (%08x)= real %20.15f Hz  emul %20.15f Hz\n",0xff-i, chip.LFOfreq[0xff-i], pom,
			(((double)chip->LFOfreq[0xff-i] / mult) * (double)chip->sampfreq ) / (double)LFO_LEN );*/
        }
        for (i = 0; i < 34; i++)
            chip.EG_tab[i] = 0;		/* infinity */

        for (i = 2; i < 64; i++) {
            pom2 = (double) chip.clock / (double) chip.sampfreq;
            if (i < 60) pom2 *= (1 + (i & 3) * 0.25);
            pom2 *= 1 << ((i >> 2));
            pom2 /= 768.0 * 1024.0;
            pom2 *= (double) (1 << ENV_SH);
            chip.EG_tab[32 + i] = (int) pom2;
        }

        for (i = 0; i < 32; i++) {
            chip.EG_tab[32 + 64 + i] = chip.EG_tab[32 + 63];
        }
			/* precalculate timers' deltas */
	/* User's Manual pages 15,16  */
        mult = (1 << TIMER_SH);
        for (i = 0; i < 1024; i++) {
		/* ASG 980324: changed to compute both TimerA and TimerATime */
            pom = (64.0 * (1024.0 - i) / (double) chip.clock);
            chip.TimerATime[i] = pom;
        }
        for (i = 0; i < 256; i++) {
		/* ASG 980324: changed to compute both TimerB and TimerBTime */
            pom = (1024.0 * (256.0 - i) / (double) chip.clock);
            chip.TimerBTime[i] = pom;
        }

	/* calculate noise periods table */
        scaler = ((double) chip.clock / 64.0) / ((double) chip.sampfreq);
        for (i = 0; i < 32; i++) {
            j = (i != 31 ? i : 30);   /*period 30 and 31 are the same*/
            j = 32 - j;
            j = (int) (65536.0 / (double) (j * 32.0));	/*number of samples per one shift of the shift register*/
		/*chip->noise_tab[i] = j * 64;*/	/*number of chip clock cycles per one shift*/
            chip.noise_tab[i] = (int) (j * 64 * scaler);
		/*logerror("noise_tab[%02x]=%08x\n", i, chip->noise_tab[i]);*/
        }

    }

    public static void envelope_KONKOFF(OscilRec[] op, int op_offset, int v) {
        if ((v & 0x08) != 0) {
            if (op[op_offset].key == 0) {
                op[op_offset].key = 1;      /*KEYON'ed*/
                op[op_offset].phase = 0;      /*clear phase */
                op[op_offset].state = EG_ATT; /*KEY ON = attack*/
            }
        } else {
            if (op[op_offset].key != 0) {
                op[op_offset].key = 0;      /*KEYOFF'ed*/
                if (op[op_offset].state > EG_REL)
                    op[op_offset].state = EG_REL; /*release*/
            }
        }

        op_offset += 8;

        if ((v & 0x20) != 0) {
            if (op[op_offset].key == 0) {
                op[op_offset].key = 1;
                op[op_offset].phase = 0;
                op[op_offset].state = EG_ATT;
            }
        } else {
            if (op[op_offset].key != 0) {
                op[op_offset].key = 0;
                if (op[op_offset].state > EG_REL)
                    op[op_offset].state = EG_REL;
            }
        }

        op_offset += 8;

        if ((v & 0x10) != 0) {
            if (op[op_offset].key == 0) {
                op[op_offset].key = 1;
                op[op_offset].phase = 0;
                op[op_offset].state = EG_ATT;
            }
        } else {
            if (op[op_offset].key != 0) {
                op[op_offset].key = 0;
                if (op[op_offset].state > EG_REL)
                    op[op_offset].state = EG_REL;
            }
        }

        op_offset += 8;

        if ((v & 0x40) != 0) {
            if (op[op_offset].key == 0) {
                op[op_offset].key = 1;
                op[op_offset].phase = 0;
                op[op_offset].state = EG_ATT;
            }
        } else {
            if (op[op_offset].key != 0) {
                op[op_offset].key = 0;
                if (op[op_offset].state > EG_REL)
                    op[op_offset].state = EG_REL;
            }
        }
    }


    public static timer_callback timer_callback_a = new timer_callback() {
        public void handler(int n) {
            _YM2151 chip = YMPSG[n];
            chip.TimATimer = timer_set(chip.TimerATime[(int) chip.TimAIndex], n, timer_callback_a);
            chip.TimAOldIndex = chip.TimAIndex;
            if ((chip.IRQenable & 0x04) != 0) {
                int oldstate = chip.status & 3;
                chip.status |= 1;
                if ((oldstate == 0) && (chip.irqhandler != null)) (chip.irqhandler).handler(1);
                if ((chip.IRQenable & 0x80) != 0)
                    chip.CSMreq = 2;	/*request KEYON/KEYOFF sequence*/
            }
        }
    };
    public static timer_callback timer_callback_b = new timer_callback() {
        public void handler(int n) {
            _YM2151 chip = YMPSG[n];
            chip.TimBTimer = timer_set(chip.TimerBTime[(int) chip.TimBIndex], n, timer_callback_b);
            chip.TimBOldIndex = chip.TimBIndex;
            if ((chip.IRQenable & 0x08) != 0) {
                int oldstate = chip.status & 3;
                chip.status |= 2;
                if ((oldstate == 0) && (chip.irqhandler) != null) (chip.irqhandler).handler(1);

            }
        }
    };

    public static void set_connect(_YM2151 chip, int op_offset, int v, int cha) {
        OscilRec om1 = chip.Oscils[op_offset];
        OscilRec om2 = chip.Oscils[op_offset + 8];
        OscilRec oc1 = chip.Oscils[op_offset + 16];
    	/*OscilRec *oc2 = om1+24;*/
    	/*oc2->connect = &chanout[cha];*/

    	/* set connect algorithm */

        switch (v & 7) {
            case 0:
    		/* M1---C1---M2---C2---OUT */
                om1.connect = c1;
                oc1.connect = m2;
                om2.connect = c2;
                break;
            case 1:
    		/* M1-+-M2---C2---OUT */
    		/* C1-+               */
                om1.connect = m2;
                oc1.connect = m2;
                om2.connect = c2;
                break;
            case 2:
    		/* M1------+-C2---OUT */
    		/* C1---M2-+          */
                om1.connect = c2;
                oc1.connect = m2;
                om2.connect = c2;
                break;
            case 3:
    		/* M1---C1-+-C2---OUT */
    		/* M2------+          */
                om1.connect = c1;
                oc1.connect = c2;
                om2.connect = c2;
                break;
            case 4:
    		/* M1---C1-+--OUT */
    		/* M2---C2-+      */
                om1.connect = c1;
                oc1.connect = chanout[cha];
                om2.connect = c2;
                break;
            case 5:
    		/*    +-C1-+     */
    		/* M1-+-M2-+-OUT */
    		/*    +-C2-+     */
                om1.connect = null;	/* special mark */
                oc1.connect = chanout[cha];
                om2.connect = chanout[cha];
                break;
            case 6:
    		/* M1---C1-+     */
    		/*      M2-+-OUT */
    		/*      C2-+     */
                om1.connect = c1;
                oc1.connect = chanout[cha];
                om2.connect = chanout[cha];
                break;
            case 7:
    		/* M1-+     */
    		/* C1-+-OUT */
    		/* M2-+     */
    		/* C2-+     */
                om1.connect = chanout[cha];
                oc1.connect = chanout[cha];
                om2.connect = chanout[cha];
                break;
        }
    }

    private static final int unsigned(int param) {
        if (param < 0) {
            return -param;
        }
        return param;
    }

    public static void refresh_EG(_YM2151 chip, OscilRec[] op, int op_offset) {
        int kc;
        int v;

    	/*v = 32 + 2*RATE + RKS (max 126)*/

        kc = unsigned(op[op_offset].KC);
        v = unsigned(kc >> op[op_offset].KS);

        if ((op[op_offset].AR + v) < 32 + 62)
            op[op_offset].delta_AR = chip.EG_tab[op[op_offset].AR + v];
        else
            op[op_offset].delta_AR = MAX_ATT_INDEX + 1;
        op[op_offset].delta_D1R = chip.EG_tab[op[op_offset].D1R + v];
        op[op_offset].delta_D2R = chip.EG_tab[op[op_offset].D2R + v];
        op[op_offset].delta_RR = chip.EG_tab[op[op_offset].RR + v];

        op_offset += 8;

        v = unsigned(kc >> op[op_offset].KS);
        if ((op[op_offset].AR + v) < 32 + 62)
            op[op_offset].delta_AR = chip.EG_tab[op[op_offset].AR + v];
        else
            op[op_offset].delta_AR = MAX_ATT_INDEX + 1;
        op[op_offset].delta_D1R = chip.EG_tab[op[op_offset].D1R + v];
        op[op_offset].delta_D2R = chip.EG_tab[op[op_offset].D2R + v];
        op[op_offset].delta_RR = chip.EG_tab[op[op_offset].RR + v];

        op_offset += 8;

        v = unsigned(kc >> op[op_offset].KS);
        if ((op[op_offset].AR + v) < 32 + 62)
            op[op_offset].delta_AR = chip.EG_tab[op[op_offset].AR + v];
        else
            op[op_offset].delta_AR = MAX_ATT_INDEX + 1;
        op[op_offset].delta_D1R = chip.EG_tab[op[op_offset].D1R + v];
        op[op_offset].delta_D2R = chip.EG_tab[op[op_offset].D2R + v];
        op[op_offset].delta_RR = chip.EG_tab[op[op_offset].RR + v];

        op_offset += 8;

        v = unsigned(kc >> op[op_offset].KS);
        if ((op[op_offset].AR + v) < 32 + 62)
            op[op_offset].delta_AR = chip.EG_tab[op[op_offset].AR + v];
        else
            op[op_offset].delta_AR = MAX_ATT_INDEX + 1;
        op[op_offset].delta_D1R = chip.EG_tab[(int) (op[op_offset].D1R + v)];
        op[op_offset].delta_D2R = chip.EG_tab[(int) (op[op_offset].D2R + v)];
        op[op_offset].delta_RR = chip.EG_tab[(int) (op[op_offset].RR + v)];

    }

    /* write a register on YM2151 chip number 'n' */
    public static void YM2151WriteReg(int n, int r, int v) {
        //System.out.println("n= "+n + " r="+r + " v="+v);
        _YM2151 chip = YMPSG[n];
        OscilRec op = chip.Oscils[r & 0x1f];

    	/*adjust bus to 8 bits*/
        r &= 0xff;
        v &= 0xff;

        switch (r & 0xe0) {
            case 0x00:
                switch (r) {
                    case 0x01: /*LFO reset(bit 1), Test Register (other bits)*/
                        chip.test = v;
                        if ((v & 2) != 0) chip.LFOphase = 0;
                        break;

                    case 0x08:
                        envelope_KONKOFF(chip.Oscils, v & 7, v);
                        break;
                    case 0x0f: /*noise mode enable, noise period*/
                        chip.noise = v;
                        chip.noise_f = chip.noise_tab[v & 0x1f];
    			/*if ((v&0x80)) printf("YM2151 noise (%02x)\n",v);*/
                        break;

                    case 0x10: /*timer A hi*/
                        chip.TimAIndex = (chip.TimAIndex & 0x003) | (v << 2);
                        break;

                    case 0x11: /*timer A low*/
                        chip.TimAIndex = (chip.TimAIndex & 0x3fc) | (v & 3);
                        break;

                    case 0x12: /*timer B*/
                        chip.TimBIndex = v;
                        break;

                    case 0x14: /*CSM, irq flag reset, irq enable, timer start/stop*/

                        chip.IRQenable = v;	/*bit 3-timer B, bit 2-timer A, bit 7 - CSM */

                        if ((v & 0x20) != 0)	/*reset timer B irq flag*/ {
                            int oldstate = chip.status & 3;
                            chip.status &= 0xfd;
                            if ((oldstate == 2) && (chip.irqhandler) != null) (chip.irqhandler).handler(0);
                        }

                        if ((v & 0x10) != 0)	/*reset timer A irq flag*/ {
                            int oldstate = chip.status & 3;
                            chip.status &= 0xfe;
                            if ((oldstate == 1) && (chip.irqhandler) != null) (chip.irqhandler).handler(0);

                        }

                        if ((v & 0x02) != 0) {	/*load and start timer B*/
    				/* ASG 980324: added a real timer */
    				/* start timer _only_ if it wasn't already started (it will reload time value next round)*/
                            if (chip.TimBTimer == null) {
                                chip.TimBTimer = timer_set(chip.TimerBTime[(int) chip.TimBIndex], n, timer_callback_b);
                                chip.TimBOldIndex = chip.TimBIndex;
                            }
                        } else {		/*stop timer B*/
    				/* ASG 980324: added a real timer */
                            if (chip.TimBTimer != null) timer_remove(chip.TimBTimer);
                            chip.TimBTimer = null;
                        }

                        if ((v & 0x01) != 0) {	/*load and start timer A*/
    				/* ASG 980324: added a real timer */
    				/* start timer _only_ if it wasn't already started (it will reload time value next round)*/
                            if (chip.TimATimer == null) {
                                chip.TimATimer = timer_set(chip.TimerATime[(int) chip.TimAIndex], n, timer_callback_a);
                                chip.TimAOldIndex = chip.TimAIndex;
                            }
                        } else {		/*stop timer A*/
    				/* ASG 980324: added a real timer */
                            if (chip.TimATimer != null) timer_remove(chip.TimATimer);
                            chip.TimATimer = null;
                        }
                        break;

                    case 0x18: /*LFO frequency*/
                        chip.LFOfrq = chip.LFOfreq[v];
                        break;

                    case 0x19: /*PMD (bit 7==1) or AMD (bit 7==0)*/
                        if ((v & 0x80) != 0)
                            chip.PMD = lfo_md_tab[v & 0x7f] + 512;
                        else
                            chip.AMD = lfo_md_tab[v & 0x7f];
                        break;

                    case 0x1b: /*CT2, CT1, LFO waveform*/
                        chip.CT = v;
                        chip.LFOwave = (v & 3) * LFO_LEN * 2;
                        if (chip.porthandler != null) (chip.porthandler).handler(0, (int) ((chip.CT) >> 6));
                        break;

                    default:
                        //logerror("YM2151 Write %02x to undocumented register #%02x\n",v,r);
                        break;
                }
                break;

            case 0x20:
                op = chip.Oscils[r & 7];
                int op_offset = r & 7;
                switch (r & 0x18) {
                    case 0x00: /*RL enable, Feedback, Connection */
                        op.FeedBack = ((v >> 3) & 7) != 0 ? ((v >> 3) & 7) + 6 : 0;
                        chip.PAN[(r & 7) * 2] = (v & 0x40) != 0 ? 0xffffffff : 0x0;
                        chip.PAN[(r & 7) * 2 + 1] = (v & 0x80) != 0 ? 0xffffffff : 0x0;
                        set_connect(chip, op_offset, v, r & 7);
                        break;

                    case 0x08: /*Key Code*/
                        v &= 0x7f;
                        if (v != op.KC) {
					/*unsigned*/
                            int kc, kc_channel;

                            kc_channel = (v - (v >> 2)) * 64;
                            kc_channel += 768;
                            kc_channel |= (op.KCindex & 63);

                            chip.Oscils[op_offset + 0].KC = v;
                            chip.Oscils[op_offset + 0].KCindex = kc_channel;
                            chip.Oscils[op_offset + 8].KC = v;
                            chip.Oscils[op_offset + 8].KCindex = kc_channel;
                            chip.Oscils[op_offset + 16].KC = v;
                            chip.Oscils[op_offset + 16].KCindex = kc_channel;
                            chip.Oscils[op_offset + 24].KC = v;
                            chip.Oscils[op_offset + 24].KCindex = kc_channel;

                            kc = v >> 2;

                            chip.Oscils[op_offset + 0].DT1v = chip.DT1freq[chip.Oscils[op_offset + 0].DT1 + kc];
                            chip.Oscils[op_offset + 0].freq = ((chip.freq[kc_channel + chip.Oscils[op_offset + 0].DT2] + chip.Oscils[op_offset + 0].DT1v) * chip.Oscils[op_offset + 0].MUL) >> 1;

                            chip.Oscils[op_offset + 8].DT1v = chip.DT1freq[chip.Oscils[op_offset + 8].DT1 + kc];
                            chip.Oscils[op_offset + 8].freq = ((chip.freq[kc_channel + chip.Oscils[op_offset + 8].DT2] + chip.Oscils[op_offset + 8].DT1v) * chip.Oscils[op_offset + 8].MUL) >> 1;

                            chip.Oscils[op_offset + 16].DT1v = chip.DT1freq[chip.Oscils[op_offset + 16].DT1 + kc];
                            chip.Oscils[op_offset + 16].freq = ((chip.freq[kc_channel + chip.Oscils[op_offset + 16].DT2] + chip.Oscils[op_offset + 16].DT1v) * chip.Oscils[op_offset + 16].MUL) >> 1;

                            chip.Oscils[op_offset + 24].DT1v = chip.DT1freq[chip.Oscils[op_offset + 24].DT1 + kc];
                            chip.Oscils[op_offset + 24].freq = ((chip.freq[kc_channel + chip.Oscils[op_offset + 24].DT2] + chip.Oscils[op_offset + 24].DT1v) * chip.Oscils[op_offset + 24].MUL) >> 1;

                            refresh_EG(chip, chip.Oscils, op_offset);
                        }
                        break;

                    case 0x10: /*Key Fraction*/
                        v >>= 2;
                        if (v != (op.KCindex & 63)) {
					/*unsigned*/
                            int kc_channel;

                            kc_channel = v;
                            kc_channel |= (op.KCindex & ~63);

                            chip.Oscils[op_offset + 0].KCindex = kc_channel;
                            chip.Oscils[op_offset + 8].KCindex = kc_channel;
                            chip.Oscils[op_offset + 16].KCindex = kc_channel;
                            chip.Oscils[op_offset + 24].KCindex = kc_channel;

                            chip.Oscils[op_offset + 0].freq = ((chip.freq[kc_channel + chip.Oscils[op_offset + 0].DT2] + chip.Oscils[op_offset + 0].DT1v) * chip.Oscils[op_offset + 0].MUL) >> 1;
                            chip.Oscils[op_offset + 8].freq = ((chip.freq[kc_channel + chip.Oscils[op_offset + 8].DT2] + chip.Oscils[op_offset + 8].DT1v) * chip.Oscils[op_offset + 8].MUL) >> 1;
                            chip.Oscils[op_offset + 16].freq = ((chip.freq[kc_channel + chip.Oscils[op_offset + 16].DT2] + chip.Oscils[op_offset + 16].DT1v) * chip.Oscils[op_offset + 16].MUL) >> 1;
                            chip.Oscils[op_offset + 24].freq = ((chip.freq[kc_channel + chip.Oscils[op_offset + 24].DT2] + chip.Oscils[op_offset + 24].DT1v) * chip.Oscils[op_offset + 24].MUL) >> 1;
                        }
                        break;

                    case 0x18: /*PMS,AMS*/
                        op.PMS = (v >> 4) & 7;
                        op.AMS = v & 3;
                        break;
                }
                break;

            case 0x40: /*DT1, MUL*/ {
                int oldDT1 = op.DT1;
                int oldMUL = op.MUL;
                op.DT1 = (v & 0x70) << 1;
                op.MUL = (v & 0x0f) != 0 ? (v & 0x0f) << 1 : 1;
                if (oldDT1 != op.DT1) {
                    op.DT1v = chip.DT1freq[op.DT1 + (op.KC >> 2)];
                }
                if ((oldDT1 != op.DT1) || (oldMUL != op.MUL)) {
                    op.freq = ((chip.freq[op.KCindex + op.DT2] + op.DT1v) * op.MUL) >> 1;
                }
            }
            break;

            case 0x60: /*TL*/
                op.TL = (v & 0x7f) << (ENV_BITS - 7); /*7bit TL*/
                break;

            case 0x80: /*KS, AR*/ {
                int oldKS = op.KS;
                int oldAR = op.AR;
                op.KS = 5 - (v >> 6);
                op.AR = (v & 0x1f) != 0 ? 32 + ((v & 0x1f) << 1) : 0;

                if ((op.AR != oldAR) || (op.KS != oldKS)) {
                    if ((op.AR + (op.KC >> op.KS)) < 32 + 62)
                        op.delta_AR = chip.EG_tab[op.AR + (op.KC >> op.KS)];
                    else
                        op.delta_AR = MAX_ATT_INDEX + 1;
                }

                if (op.KS != oldKS) {
                    op.delta_D1R = chip.EG_tab[op.D1R + (op.KC >> op.KS)];
                    op.delta_D2R = chip.EG_tab[op.D2R + (op.KC >> op.KS)];
                    op.delta_RR = chip.EG_tab[op.RR + (op.KC >> op.KS)];
                }
            }
            break;

            case 0xa0: /*AMS-EN, D1R*/
                op.AMSmask = (v & 0x80) != 0 ? 0xffffffff : 0;
                op.D1R = (v & 0x1f) != 0 ? 32 + ((v & 0x1f) << 1) : 0;
                op.delta_D1R = chip.EG_tab[op.D1R + (op.KC >> op.KS)];
                break;

            case 0xc0: /*DT2, D2R*/ {
                int oldDT2 = op.DT2;
                op.DT2 = DT2_tab[v >> 6];
                if (op.DT2 != oldDT2) {
                    op.freq = unsigned((chip.freq[op.KCindex + op.DT2] + op.DT1v) * op.MUL) >> 1;
                }
            }
            op.D2R = (v & 0x1f) != 0 ? 32 + ((v & 0x1f) << 1) : 0;
            op.delta_D2R = chip.EG_tab[(int) (op.D2R + (op.KC >> op.KS))];
            break;

            case 0xe0: /*D1L, RR*/
                op.D1L = D1L_tab[v >> 4];
                op.RR = 34 + ((v & 0x0f) << 2);
                op.delta_RR = chip.EG_tab[(int) (op.RR + (op.KC >> op.KS))];
                break;
        }
    }

    public static int YM2151ReadStatus(int n) {
        return YMPSG[n].status;
    }

    /*
    ** Initialize YM2151 emulator(s).
    **
    ** 'num' is the number of virtual YM2151's to allocate
    ** 'clock' is the chip clock in Hz
    ** 'rate' is sampling rate
    */
    public static int YM2151Init(int num, int clock, int rate) {
        int i;

        if (YMPSG != null) return (-1);	/* duplicate init. */

        YMNumChips = num;

        YMPSG = new _YM2151[YMNumChips];//(YM2151 *)malloc(sizeof(YM2151) * YMNumChips);
        if (YMPSG == null) return (1);
        //memset(YMPSG, 0, sizeof(YM2151) * YMNumChips);
        init_tables();
        for (i = 0; i < YMNumChips; i++) {
            YMPSG[i] = new _YM2151();
            YMPSG[i].clock = clock;
            /*rate = clock/64;*/
            YMPSG[i].sampfreq = rate != 0 ? rate : 44100;	/* avoid division by 0 in init_chip_tables() */
            YMPSG[i].irqhandler = null;					/*interrupt handler */
            YMPSG[i].porthandler = null;				/*port write handler*/
            init_chip_tables(YMPSG[i]);
            YMPSG[i].TimATimer = null;
            YMPSG[i].TimBTimer = null;
            YM2151ResetChip(i);
        	/*logerror("YM2151[init] clock=%i sampfreq=%i\n", YMPSG[i].clock, YMPSG[i].sampfreq);*/
        }
        return (0);
    }

    public static void YM2151Shutdown() {
        if (YMPSG == null) return;
        YMPSG = null;
    }


    /*
    ** reset all chip registers.
    */
    public static void YM2151ResetChip(int num) {
        int i;
        _YM2151 chip = YMPSG[num];

    	/* initialize hardware registers */

        for (i = 0; i < 32; i++) {
            chip.Oscils[i] = new OscilRec();//memset(&chip->Oscils[i],'\0',sizeof(OscilRec));
            chip.Oscils[i].volume = MAX_ATT_INDEX;
        }

        chip.LFOphase = 0;
        chip.LFOfrq = 0;
        chip.LFOwave = 0;
        chip.PMD = lfo_md_tab[0] + 512;
        chip.AMD = lfo_md_tab[0];
        chip.LFA = 0;
        chip.LFP = 0;

        chip.test = 0;

        chip.IRQenable = 0;

    	/* ASG 980324 -- reset the timers before writing to the registers */
        if (chip.TimATimer != null) timer_remove(chip.TimATimer);
        chip.TimATimer = null;
        if (chip.TimBTimer != null) timer_remove(chip.TimBTimer);
        chip.TimBTimer = null;
        chip.TimAIndex = 0;
        chip.TimBIndex = 0;
        chip.TimAOldIndex = 0;
        chip.TimBOldIndex = 0;

        chip.noise = 0;
        chip.noiseRNG = 0;
        chip.noise_p = 0;
        chip.noise_f = chip.noise_tab[0];

        chip.CSMreq = 0;

        chip.status = 0;

        YM2151WriteReg(num, 0x1b, 0); /*only because of CT1, CT2 output pins*/
        for (i = 0x20; i < 0x100; i++)   /*just to set the PM operators */ {
            YM2151WriteReg(num, i, 0);
        }

    }

    public static void lfo_calc() {
        int phase, lfx;

        if ((PSG.test & 2) != 0) {
            PSG.LFOphase = 0;
            phase = unsigned(PSG.LFOwave);
        } else {
            phase = unsigned((PSG.LFOphase >> LFO_SH) & LFO_MASK);
            phase = unsigned(phase * 2 + PSG.LFOwave);
        }

        lfx = unsigned(lfo_tab[phase] + PSG.AMD);

        PSG.LFA = 0;
        if (lfx < TL_TAB_LEN)
            PSG.LFA = TL_TAB[lfx];

        lfx = unsigned(lfo_tab[(int) (phase + 1)] + PSG.PMD);

        PSG.LFP = 0;
        if (lfx < TL_TAB_LEN)
            PSG.LFP = TL_TAB[lfx];
    }


    public static void calc_lfo_pm(OscilRec[] OP, int op_offset) {
        int mod_ind, pom;

        mod_ind = PSG.LFP; /* -128..+127 (8bits signed)*/
        if (OP[op_offset].PMS < 6)
            mod_ind >>= (6 - OP[op_offset].PMS);
        else
            mod_ind <<= (OP[op_offset].PMS - 5);
        if (mod_ind != 0) {
			/*unsigned*/
            int kc_channel;

            kc_channel = unsigned(OP[op_offset].KCindex + mod_ind);

            pom = ((PSG.freq[kc_channel + OP[op_offset].DT2] + OP[op_offset].DT1v) * OP[op_offset].MUL) >> 1;
            OP[op_offset].phase += (pom - OP[op_offset].freq);

            op_offset += 8;
            pom = ((PSG.freq[kc_channel + OP[op_offset].DT2] + OP[op_offset].DT1v) * OP[op_offset].MUL) >> 1;
            OP[op_offset].phase += (pom - OP[op_offset].freq);

            op_offset += 8;
            pom = ((PSG.freq[kc_channel + OP[op_offset].DT2] + OP[op_offset].DT1v) * OP[op_offset].MUL) >> 1;
            OP[op_offset].phase += (pom - OP[op_offset].freq);

            op_offset += 8;
            pom = ((PSG.freq[kc_channel + OP[op_offset].DT2] + OP[op_offset].DT1v) * OP[op_offset].MUL) >> 1;
            OP[op_offset].phase += (pom - OP[op_offset].freq);
        }

    }

    public static int op_calc(OscilRec[] OP, int op_offset, /*unsigned*/ int env, int pm) {
        int p;

        p = unsigned((env << 3) + sin_tab[((((OP[op_offset].phase & ~FREQ_MASK) + (pm << 15))) >> FREQ_SH) & SIN_MASK]);

        if (p >= TL_TAB_LEN)
            return 0;

        return TL_TAB[(int) p];
    }

    public static int op_calc1(OscilRec[] OP, int op_offset, /*unsigned*/ int env, int pm) {
        int p;
        int i;

        i = ((OP[op_offset].phase & ~FREQ_MASK) + pm);

    /*if (errorlog) fprintf(errorlog,"i=%08x (i>>16)&511=%8i phase=%i [pm=%08x] ",i, (i>>16)&511, OP->phase>>FREQ_SH, pm);*/

        p = unsigned((env << 3) + sin_tab[(i >> FREQ_SH) & SIN_MASK]);

    /*if (errorlog) fprintf(errorlog," (p&255=%i p>>8=%i) out= %i\n", p&255,p>>8, TL_TAB[p&255]>>(p>>8) );*/

        if (p >= TL_TAB_LEN)
            return 0;

        return TL_TAB[(int) p];
    }

    private static int volume_calc(OscilRec OP, int AM) {
        return (OP.TL + (unsigned(OP.volume) >> ENV_SH) + (AM & OP.AMSmask));
    }

    public static void chan_calc(int chan) {
        OscilRec[] OP = PSG.Oscils;
        int env;
        int AM;

        chanout[chan][0] = 0;
        c1[0] = 0;
        m2[0] = 0;
        c2[0] = 0;
        AM = 0;

        //OP = &PSG->Oscils[chan]; /*M1*/
        int op_offset = chan;

        if (PSG.Oscils[op_offset].AMS != 0)
            AM = unsigned(PSG.LFA << (PSG.Oscils[op_offset].AMS - 1));

        if (PSG.Oscils[op_offset].PMS != 0)
            calc_lfo_pm(PSG.Oscils, op_offset);

        env = unsigned(volume_calc(OP[op_offset], AM));
        {
            int out;

            out = PSG.Oscils[op_offset].FB0 + PSG.Oscils[op_offset].FB;
            PSG.Oscils[op_offset].FB0 = PSG.Oscils[op_offset].FB;

            if (PSG.Oscils[op_offset].connect == null)
    			/* algorithm 5 */
                c1[0] = m2[0] = c2[0] = PSG.Oscils[op_offset].FB0;
            else
    			/* other algorithms */
                PSG.Oscils[op_offset].connect[0] = PSG.Oscils[op_offset].FB0;

            PSG.Oscils[op_offset].FB = 0;

            if (env < ENV_QUIET)
                PSG.Oscils[op_offset].FB = op_calc1(PSG.Oscils, op_offset, (int) env, (out << PSG.Oscils[op_offset].FeedBack));
        }

        op_offset += 16; /*C1*/
        env = unsigned(volume_calc(OP[op_offset], AM));
        if (env < ENV_QUIET)
            PSG.Oscils[op_offset].connect[0] += op_calc(PSG.Oscils, op_offset, env, c1[0]);

        op_offset -= 8;  /*M2*/
        env = unsigned(volume_calc(OP[op_offset], AM));
        if (env < ENV_QUIET)
            PSG.Oscils[op_offset].connect[0] += op_calc(PSG.Oscils, op_offset, env, m2[0]);

        op_offset += 16; /*C2*/
        env = unsigned(volume_calc(OP[op_offset], AM));
        if (env < ENV_QUIET)
            chanout[chan][0] += op_calc(PSG.Oscils, op_offset, env, c2[0]);

    }

    public static void chan7_calc() {
        OscilRec[] OP = PSG.Oscils;
		/*unsigned*/
        int env;
		/*unsigned*/
        int AM;
        int op_offset = 0;

        chanout[7][0] = 0;
        c1[0] = 0;
        m2[0] = 0;
        c2[0] = 0;
        AM = 0;

        op_offset = 7;//OP = PSG.Oscils[7]; /*M1*/

        if (OP[op_offset].AMS != 0)
            AM = PSG.LFA << (OP[op_offset].AMS - 1);

        if (OP[op_offset].PMS != 0)
            calc_lfo_pm(OP, op_offset);

        env = volume_calc(OP[op_offset], AM);
        {
            int out;

            out = OP[op_offset].FB0 + OP[op_offset].FB;
            OP[op_offset].FB0 = OP[op_offset].FB;

            if (OP[op_offset].connect == null)
			/* algorithm 5 */
                c1[0] = m2[0] = c2[0] = OP[op_offset].FB0;
            else {
			/* other algorithms */
                OP[op_offset].connect[0] = OP[op_offset].FB0;
            }

            OP[op_offset].FB = 0;

            if (env < ENV_QUIET)
                OP[op_offset].FB = op_calc1(OP, op_offset, env, (out << OP[op_offset].FeedBack));
        }

        op_offset += 16; /*C1*/
        env = volume_calc(OP[op_offset], AM);
        if (env < ENV_QUIET)
            OP[op_offset].connect[0] += op_calc(OP, op_offset, env, c1[0]);

        op_offset -= 8;  /*M2*/
        env = volume_calc(OP[op_offset], AM);
        if (env < ENV_QUIET)
            OP[op_offset].connect[0] += op_calc(OP, op_offset, env, m2[0]);

        op_offset += 16; /*C2*/
        env = volume_calc(OP[op_offset], AM);

        if ((PSG.noise & 0x80) != 0) {
			/*unsigned*/
            int noiseout;

            noiseout = 0;
            if (env < 0x3ff)
                noiseout = (env ^ 0x3ff) * 2; /*range of the YM2151 noise output is -2044 to 2040*/
            chanout[7][0] += ((PSG.noiseRNG & 0x10000) != 0 ? noiseout : -noiseout); /*bit 16 -> output*/
        } else {
            if (env < ENV_QUIET)
                chanout[7][0] += op_calc(OP, op_offset, env, c2[0]);
        }

    }

    public static void advance() {
        OscilRec[] op = PSG.Oscils;
        int i;
        int op_offset = 0;

        if ((PSG.test & 2) == 0)
            PSG.LFOphase += PSG.LFOfrq;

	/* The Noise Generator of the YM2151 is 17-bit shift register.
	** Input to the bit16 is negated (bit0 XOR bit3) (EXNOR).
	** Output of the register is negated (bit0 XOR bit3).
	** Simply use bit16 as the noise output.
	*/
        PSG.noise_p += PSG.noise_f;
        i = (PSG.noise_p >> 16);		/*number of events (shifts of the shift register)*/
        PSG.noise_p &= 0xffff;
        while (i != 0) {
			/*unsigned*/
            int j;
            j = ((PSG.noiseRNG ^ (PSG.noiseRNG >> 3)) & 1) ^ 1;
            PSG.noiseRNG = (j << 16) | (PSG.noiseRNG >> 1);
            i--;
        }

	/* In real it seems that CSM keyon line is ORed with the KO line inside of the chip.
	** This causes it to only work when KO is off, ie. 0
	** Below is my implementation only.
	*/
        if (PSG.CSMreq != 0) /*CSM KEYON/KEYOFF seqeunce request*/ {
            if (PSG.CSMreq == 2)	/*KEYON*/ {
                op_offset = 0;//op = &PSG->Oscils[0]; /*CH 0 M1*/
                i = 32;
                do {
                    if (op[op_offset].key == 0) /*_ONLY_ when KEY is OFF (checked)*/ {
                        op[op_offset].phase = 0;
                        op[op_offset].state = EG_ATT;
                    }
                    op_offset++;
                    i--;
                } while (i != 0);
                PSG.CSMreq = 1;
            } else					/*KEYOFF*/ {
                op_offset = 0;//op = &PSG->Oscils[0]; /*CH 0 M1*/
                i = 32;
                do {
                    if (op[op_offset].key == 0) /*_ONLY_ when KEY is OFF (checked)*/ {
                        if (op[op_offset].state > EG_REL)
                            op[op_offset].state = EG_REL;
                    }
                    op_offset++;
                    i--;
                } while (i != 0);
                PSG.CSMreq = 0;
            }
        }

        op_offset = 0;//op = &PSG->Oscils[0]; /*CH0 M1*/
        i = 32;
        do {
            op[op_offset].phase += op[op_offset].freq;

            switch (op[op_offset].state) {
                case EG_ATT:	/*attack phase*/ {
					/*signed*/
                    int step;

                    step = op[op_offset].volume;
                    op[op_offset].volume -= op[op_offset].delta_AR;
                    step = (step >> ENV_SH) - ((unsigned(op[op_offset].volume)) >> ENV_SH); /*number of levels passed since last time*/
                    if (step > 0) {
                        int tmp_volume;

                        tmp_volume = op[op_offset].volume + (step << ENV_SH); /*adjust by number of levels*/
                        do {
                            tmp_volume = tmp_volume - (1 << ENV_SH) - ((tmp_volume >> 4) & ~ENV_MASK);
                            if (tmp_volume <= MIN_ATT_INDEX)
                                break;
                            step--;
                        } while (step != 0);
                        op[op_offset].volume = tmp_volume;
                    }

                    if (op[op_offset].volume <= MIN_ATT_INDEX) {
                        if (op[op_offset].volume < 0)
                            op[op_offset].volume = 0; /*this is not quite correct (checked)*/
                        op[op_offset].state = EG_DEC;
                    }
                }
                break;

                case EG_DEC:	/*decay phase*/
                    if ((op[op_offset].volume += op[op_offset].delta_D1R) >= op[op_offset].D1L) {
                        op[op_offset].volume = op[op_offset].D1L; /*this is not quite correct (checked)*/
                        op[op_offset].state = EG_SUS;
                    }
                    break;

                case EG_SUS:	/*sustain phase*/
                    if ((op[op_offset].volume += op[op_offset].delta_D2R) > MAX_ATT_INDEX) {
                        op[op_offset].state = EG_OFF;
                        op[op_offset].volume = MAX_ATT_INDEX;
                    }
                    break;

                case EG_REL:	/*release phase*/
                    if ((op[op_offset].volume += op[op_offset].delta_RR) > MAX_ATT_INDEX) {
                        op[op_offset].state = EG_OFF;
                        op[op_offset].volume = MAX_ATT_INDEX;
                    }
                    break;
            }
            op_offset++;
            i--;
        } while (i != 0);
    }

    /*
    ** Generate samples for one of the YM2151's
    **
    ** 'num' is the number of virtual YM2151
    ** '**buffers' is table of pointers to the buffers: left and right
    ** 'length' is the number of samples that should be generated
    */
    public static StreamInitMultiPtr YM2151UpdateOne = new StreamInitMultiPtr() {
        public void handler(int num, ShortPtr[] buffer, int length) {
            int i;
            int outl, outr;
            ShortPtr bufL, bufR;
            bufL = buffer[0];
            bufR = buffer[1];

            PSG = YMPSG[num];//	PSG = &YMPSG[num];

            for (i = 0; i < length; i++) {

                chan_calc(0);
                chan_calc(1);
                chan_calc(2);
                chan_calc(3);
                chan_calc(4);
                chan_calc(5);
                chan_calc(6);
                chan7_calc();
                outl = (chanout[0][0] & PSG.PAN[0]);
                outr = (chanout[0][0] & PSG.PAN[1]);
                outl += ((chanout[1][0] & PSG.PAN[2]));
                outr += ((chanout[1][0] & PSG.PAN[3]));
                outl += ((chanout[2][0] & PSG.PAN[4]));
                outr += ((chanout[2][0] & PSG.PAN[5]));
                outl += ((chanout[3][0] & PSG.PAN[6]));
                outr += ((chanout[3][0] & PSG.PAN[7]));
                outl += ((chanout[4][0] & PSG.PAN[8]));
                outr += ((chanout[4][0] & PSG.PAN[9]));
                outl += ((chanout[5][0] & PSG.PAN[10]));
                outr += ((chanout[5][0] & PSG.PAN[11]));
                outl += ((chanout[6][0] & PSG.PAN[12]));
                outr += ((chanout[6][0] & PSG.PAN[13]));
                outl += ((chanout[7][0] & PSG.PAN[14]));
                outr += ((chanout[7][0] & PSG.PAN[15]));

                outl >>= FINAL_SH;
                outr >>= FINAL_SH;
                if (outl > MAXOUT) outl = MAXOUT;
                else if (outl < MINOUT) outl = MINOUT;
                if (outr > MAXOUT) outr = MAXOUT;
                else if (outr < MINOUT) outr = MINOUT;
                bufL.write(i, (short) (outl));
                bufR.write(i, (short) (outr));

                lfo_calc();
                advance();

            }
        }
    };

    public static void YM2151SetIrqHandler(int n, WriteYmHandlerPtr handler) {
        YMPSG[n].irqhandler = handler;
    }

    public static void YM2151SetPortWriteHandler(int n, WriteHandlerPtr handler) {
        YMPSG[n].porthandler = handler;
    }
}
