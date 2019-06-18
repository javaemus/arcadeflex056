/**
 * ported to 0.56
 */
package arcadeflex056;

import arcadeflex036.osdepend;
import static arcadeflex036.osdepend.*;
import static common.ptr.*;
import static common.util.*;
import static common.libc.cstdio.*;
import static common.libc.cstring.*;
import java.io.File;
import static mame056.mame.mame_highscore_enabled;

import static mame056.osdependH.*;

public class fileio {

    /*TODO*////* Verbose outputs to error.log ? */
/*TODO*///#define VERBOSE 	0
/*TODO*///
/*TODO*////* Use the file cache ? */
/*TODO*///#define FILE_CACHE	1
/*TODO*///
/*TODO*///#if VERBOSE
/*TODO*///#define LOG(x)	logerror x
/*TODO*///#else
/*TODO*///#define LOG(x)	/* x */
/*TODO*///#endif
/*TODO*///
/*TODO*///static char **rompathv = NULL;
/*TODO*///static int rompathc = 0;
/*TODO*///static int rompath_needs_decomposition = 1;
/*TODO*///extern char *rompath_extra;
/*TODO*///
/*TODO*///static char **samplepathv = NULL;
/*TODO*///static int samplepathc = 0;
/*TODO*///static int samplepath_needs_decomposition = 1;
/*TODO*///
/*TODO*///static const char *rompath;
/*TODO*///static const char *samplepath;
/*TODO*///static const char *cfgdir, *nvdir, *hidir, *inpdir, *stadir;
    /*HACK*/
    static String nvdir = "nvram";
    /*HACK*/
    static String hidir = "hi";
    static String memcarddir, artworkdir="artwork", screenshotdir, cheatdir;
/*TODO*////* from datafile.c */
/*TODO*///extern const char *history_filename;
/*TODO*///extern const char *mameinfo_filename;
/*TODO*////* from cheat.c */
/*TODO*///extern char *cheatfile;
/*TODO*///
/*TODO*///static int request_decompose_rompath(struct rc_option *option, const char *arg, int priority);
/*TODO*///static int request_decompose_samplepath(struct rc_option *option, const char *arg, int priority);
/*TODO*///
/*TODO*///struct rc_option fileio_opts[] =
/*TODO*///{
/*TODO*///	/* name, shortname, type, dest, deflt, min, max, func, help */
/*TODO*///	{ "Windows path and directory options", NULL, rc_seperator, NULL, NULL, 0, 0, NULL, NULL },
/*TODO*///	{ "rompath", "rp", rc_string, &rompath, "roms", 0, 0, request_decompose_rompath, "path to romsets" },
/*TODO*///	{ "samplepath", "sp", rc_string, &samplepath, "samples", 0, 0, request_decompose_samplepath, "path to samplesets" },
/*TODO*///	{ "cfg_directory", NULL, rc_string, &cfgdir, "cfg", 0, 0, NULL, "directory to save configurations" },
/*TODO*///	{ "nvram_directory", NULL, rc_string, &nvdir, "nvram", 0, 0, NULL, "directory to save nvram contents" },
/*TODO*///	{ "memcard_directory", NULL, rc_string, &memcarddir, "memcard", 0, 0, NULL, "directory to save memory card contents" },
/*TODO*///	{ "input_directory", NULL, rc_string, &inpdir, "inp", 0, 0, NULL, "directory to save input device logs" },
/*TODO*///	{ "hiscore_directory", NULL, rc_string, &hidir, "hi", 0, 0, NULL, "directory to save hiscores" },
/*TODO*///	{ "state_directory", NULL, rc_string, &stadir, "sta", 0, 0, NULL, "directory to save states" },
/*TODO*///	{ "artwork_directory", NULL, rc_string, &artworkdir, "artwork", 0, 0, NULL, "directory for Artwork (Overlays etc.)" },
/*TODO*///	{ "snapshot_directory", NULL, rc_string, &screenshotdir, "snap", 0, 0, NULL, "directory for screenshots (.png format)" },
/*TODO*///	{ "cheat_file", NULL, rc_string, &cheatfile, "cheat.dat", 0, 0, NULL, "cheat filename" },
/*TODO*///	{ "history_file", NULL, rc_string, &history_filename, "history.dat", 0, 0, NULL, NULL },
/*TODO*///	{ "mameinfo_file", NULL, rc_string, &mameinfo_filename, "mameinfo.dat", 0, 0, NULL, NULL },
/*TODO*///	{ NULL,	NULL, rc_end, NULL, NULL, 0, 0,	NULL, NULL }
/*TODO*///};
/*TODO*///
/*TODO*///
/*TODO*///char *alternate_name;	/* for "-romdir" */
/*TODO*///
    public static final int kPlainFile = 0;
    public static final int kRAMFile = 1;
    public static final int kZippedFile = 2;

    public static class FakeFileHandle {

        public FILE file;
        public char[] data = new char[1];
        public /*unsigned*/ int offset;
        public /*unsigned*/ int length;
        public int type;
        public /*unsigned*/ int crc;
    }
    
    public static void memmove(UBytePtr dest, UBytePtr src, int numBytes){
        for (int i=0 ; i<numBytes ; i++)
            dest.write(src.read(i));
    }

    /*TODO*////*
/*TODO*/// * File stat cache LRU (Last Recently Used)
/*TODO*/// */
/*TODO*///
/*TODO*///#if FILE_CACHE
/*TODO*///struct file_cache_entry
/*TODO*///{
/*TODO*///	struct stat stat_buffer;
/*TODO*///	int result;
/*TODO*///	char *file;
/*TODO*///};
/*TODO*///
/*TODO*////* File cache buffer */
/*TODO*///static struct file_cache_entry **file_cache_map = 0;
/*TODO*///
/*TODO*////* File cache size */
/*TODO*///static unsigned int file_cache_max = 0;
/*TODO*///
/*TODO*////* AM 980919 */
/*TODO*///static int cache_stat (const char *path, struct stat *statbuf)
/*TODO*///{
/*TODO*///	if( file_cache_max )
/*TODO*///	{
/*TODO*///		unsigned i;
/*TODO*///		struct file_cache_entry *entry;
/*TODO*///
/*TODO*///		/* search in the cache */
/*TODO*///		for( i = 0; i < file_cache_max; ++i )
/*TODO*///		{
/*TODO*///			if( file_cache_map[i]->file && strcmp (file_cache_map[i]->file, path) == 0 )
/*TODO*///			{	/* found */
/*TODO*///				unsigned j;
/*TODO*///
/*TODO*/////				LOG(("File cache HIT  for %s\n", path));
/*TODO*///				/* store */
/*TODO*///				entry = file_cache_map[i];
/*TODO*///
/*TODO*///				/* shift */
/*TODO*///				for( j = i; j > 0; --j )
/*TODO*///					file_cache_map[j] = file_cache_map[j - 1];
/*TODO*///
/*TODO*///				/* set the first entry */
/*TODO*///				file_cache_map[0] = entry;
/*TODO*///
/*TODO*///				if( entry->result == 0 )
/*TODO*///					memcpy (statbuf, &entry->stat_buffer, sizeof (struct stat));
/*TODO*///
/*TODO*///				return entry->result;
/*TODO*///			}
/*TODO*///		}
/*TODO*/////		LOG(("File cache FAIL for %s\n", path));
/*TODO*///
/*TODO*///		/* oldest entry */
/*TODO*///		entry = file_cache_map[file_cache_max - 1];
/*TODO*///		free (entry->file);
/*TODO*///
/*TODO*///		/* shift */
/*TODO*///		for( i = file_cache_max - 1; i > 0; --i )
/*TODO*///			file_cache_map[i] = file_cache_map[i - 1];
/*TODO*///
/*TODO*///		/* set the first entry */
/*TODO*///		file_cache_map[0] = entry;
/*TODO*///
/*TODO*///		/* file */
/*TODO*///		entry->file = (char *) malloc (strlen (path) + 1);
/*TODO*///		strcpy (entry->file, path);
/*TODO*///
/*TODO*///		/* result and stat */
/*TODO*///		entry->result = stat (path, &entry->stat_buffer);
/*TODO*///
/*TODO*///		if( entry->result == 0 )
/*TODO*///			memcpy (statbuf, &entry->stat_buffer, sizeof (struct stat));
/*TODO*///
/*TODO*///		return entry->result;
/*TODO*///	}
/*TODO*///	else
/*TODO*///	{
/*TODO*///		return stat (path, statbuf);
/*TODO*///	}
/*TODO*///}
/*TODO*///
/*TODO*////* AM 980919 */
/*TODO*///static void cache_allocate (unsigned entries)
/*TODO*///{
/*TODO*///	if( entries )
/*TODO*///	{
/*TODO*///		unsigned i;
/*TODO*///
/*TODO*///		file_cache_max = entries;
/*TODO*///		file_cache_map = (struct file_cache_entry **) malloc (file_cache_max * sizeof (struct file_cache_entry *));
/*TODO*///
/*TODO*///		for( i = 0; i < file_cache_max; ++i )
/*TODO*///		{
/*TODO*///			file_cache_map[i] = (struct file_cache_entry *) malloc (sizeof (struct file_cache_entry));
/*TODO*///			memset (file_cache_map[i], 0, sizeof (struct file_cache_entry));
/*TODO*///		}
/*TODO*///		LOG(("File cache allocated for %d entries\n", file_cache_max));
/*TODO*///	}
/*TODO*///}
/*TODO*///#else
/*TODO*///
/*TODO*///#define cache_stat(a,b) stat(a,b)
/*TODO*///
/*TODO*///#endif
/*TODO*///
/*TODO*///static int request_decompose_rompath(struct rc_option *option, const char *arg, int priority)
/*TODO*///{
/*TODO*///	rompath_needs_decomposition = 1;
/*TODO*///
/*TODO*///	option->priority = priority;
/*TODO*///	return 0;
/*TODO*///}
/*TODO*///
/*TODO*////* rompath will be decomposed only once after all configuration
/*TODO*/// * options are parsed */
/*TODO*///static void decompose_rompath(void)
/*TODO*///{
/*TODO*///	char *token;
/*TODO*///	static char* path;
/*TODO*///
/*TODO*///	LOG(("decomposing rompath\n"));
/*TODO*///	if (rompath_extra)
/*TODO*///		LOG(("  rompath_extra = %s\n", rompath_extra));
/*TODO*///	LOG(("  rompath = %s\n", rompath));
/*TODO*///
/*TODO*///	/* run only once */
/*TODO*///	rompath_needs_decomposition = 0;
/*TODO*///
/*TODO*///	/* start with zero path components */
/*TODO*///	rompathc = 0;
/*TODO*///
/*TODO*///	if (rompath_extra)
/*TODO*///	{
/*TODO*///		rompathv = malloc (sizeof(char *));
/*TODO*///		rompathv[rompathc++] = rompath_extra;
/*TODO*///	}
/*TODO*///
/*TODO*///	if (!path)
/*TODO*///		path = malloc( strlen(rompath) + 1);
/*TODO*///	else
/*TODO*///		path = realloc( path, strlen(rompath) + 1);
/*TODO*///
/*TODO*///	if( !path )
/*TODO*///	{
/*TODO*///		logerror("decompose_rom_path: failed to malloc!\n");
/*TODO*///		raise(SIGABRT);
/*TODO*///	}
/*TODO*///
/*TODO*///	strcpy (path, rompath);
/*TODO*///	token = strtok (path, ";");
/*TODO*///	while( token )
/*TODO*///	{
/*TODO*///		if( rompathc )
/*TODO*///			rompathv = realloc (rompathv, (rompathc + 1) * sizeof(char *));
/*TODO*///		else
/*TODO*///			rompathv = malloc (sizeof(char *));
/*TODO*///		if( !rompathv )
/*TODO*///			break;
/*TODO*///		rompathv[rompathc++] = token;
/*TODO*///		token = strtok (NULL, ";");
/*TODO*///	}
/*TODO*///
/*TODO*///#if FILE_CACHE
/*TODO*///	/* AM 980919 */
/*TODO*///	if( file_cache_max == 0 )
/*TODO*///	{
/*TODO*///		/* (rom path directories + 1 buffer)==rompathc+1 */
/*TODO*///		/* (dir + .zip + .zif)==3 */
/*TODO*///		/* (clone+parent)==2 */
/*TODO*///		cache_allocate ((rompathc + 1) * 3 * 2);
/*TODO*///	}
/*TODO*///#endif
/*TODO*///}
/*TODO*///
/*TODO*///static int request_decompose_samplepath(struct rc_option *option, const char *arg, int priority)
/*TODO*///{
/*TODO*///	samplepath_needs_decomposition = 1;
/*TODO*///
/*TODO*///	option->priority = priority;
/*TODO*///	return 0;
/*TODO*///}
/*TODO*///
/*TODO*////* samplepath will be decomposed only once after all configuration
/*TODO*/// * options are parsed */
/*TODO*///static void decompose_samplepath(void)
/*TODO*///{
/*TODO*///	char *token;
/*TODO*///	static char *path;
/*TODO*///
/*TODO*///	LOG(("decomposing samplepath\n  samplepath = %s\n", samplepath));
/*TODO*///
/*TODO*///	/* run only once */
/*TODO*///	samplepath_needs_decomposition = 0;
/*TODO*///
/*TODO*///	/* start with zero path components */
/*TODO*///	samplepathc = 0;
/*TODO*///
/*TODO*///	if (!path)
/*TODO*///		path = malloc( strlen(samplepath) + 1);
/*TODO*///	else
/*TODO*///		path = realloc( path, strlen(samplepath) + 1);
/*TODO*///
/*TODO*///	if( !path )
/*TODO*///	{
/*TODO*///		logerror("decompose_sample_path: failed to malloc!\n");
/*TODO*///		raise(SIGABRT);
/*TODO*///	}
/*TODO*///
/*TODO*///	strcpy (path, samplepath);
/*TODO*///	token = strtok (path, ";");
/*TODO*///	while( token )
/*TODO*///	{
/*TODO*///		if( samplepathc )
/*TODO*///			samplepathv = realloc (samplepathv, (samplepathc + 1) * sizeof(char *));
/*TODO*///		else
/*TODO*///			samplepathv = malloc (sizeof(char *));
/*TODO*///		if( !samplepathv )
/*TODO*///			break;
/*TODO*///		samplepathv[samplepathc++] = token;
/*TODO*///		token = strtok (NULL, ";");
/*TODO*///	}
/*TODO*///}
/*TODO*///
/*TODO*///static inline void decompose_paths_if_needed(void)
/*TODO*///{
/*TODO*///	if (rompath_needs_decomposition)
/*TODO*///		decompose_rompath();
/*TODO*///	if (samplepath_needs_decomposition)
/*TODO*///		decompose_samplepath();
/*TODO*///}
/*TODO*///
/*TODO*////*
/*TODO*/// * file handling routines
/*TODO*/// *
/*TODO*/// * gamename holds the driver name, filename is only used for ROMs and samples.
/*TODO*/// * if 'write' is not 0, the file is opened for write. Otherwise it is opened
/*TODO*/// * for read.
/*TODO*/// */
/*TODO*///
/*TODO*////*
/*TODO*/// * check if roms/samples for a game exist at all
/*TODO*/// * return index+1 of the path vector component on success, otherwise 0
/*TODO*/// */
/*TODO*///int osd_faccess (const char *newfilename, int filetype)
/*TODO*///{
/*TODO*///	static int indx;
/*TODO*///	static const char *filename;
/*TODO*///	char name[256];
/*TODO*///	char **pathv;
/*TODO*///	int pathc;
/*TODO*///	char *dir_name;
/*TODO*///
/*TODO*///	/* update path info */
/*TODO*///	decompose_paths_if_needed();
/*TODO*///
/*TODO*///	/* if filename == NULL, continue the search */
/*TODO*///	if( newfilename != NULL )
/*TODO*///	{
/*TODO*///		indx = 0;
/*TODO*///		filename = newfilename;
/*TODO*///	}
/*TODO*///	else
/*TODO*///		indx++;
/*TODO*///
/*TODO*///	if( filetype == OSD_FILETYPE_ROM )
/*TODO*///	{
/*TODO*///		pathv = rompathv;
/*TODO*///		pathc = rompathc;
/*TODO*///	}
/*TODO*///	else
/*TODO*///	if( filetype == OSD_FILETYPE_SAMPLE )
/*TODO*///	{
/*TODO*///		pathv = samplepathv;
/*TODO*///		pathc = samplepathc;
/*TODO*///	}
/*TODO*///	else
/*TODO*///	if( filetype == OSD_FILETYPE_SCREENSHOT )
/*TODO*///	{
/*TODO*///		void *f;
/*TODO*///
/*TODO*///		sprintf (name, "%s/%s.png", screenshotdir, newfilename);
/*TODO*///		f = fopen (name, "rb");
/*TODO*///		if( f )
/*TODO*///		{
/*TODO*///			fclose (f);
/*TODO*///			return 1;
/*TODO*///		}
/*TODO*///		else
/*TODO*///			return 0;
/*TODO*///	}
/*TODO*///	else
/*TODO*///		return 0;
/*TODO*///
/*TODO*///	for( ; indx < pathc; indx++ )
/*TODO*///	{
/*TODO*///		struct stat stat_buffer;
/*TODO*///
/*TODO*///		dir_name = pathv[indx];
/*TODO*///
/*TODO*///		/* does such a directory (or file) exist? */
/*TODO*///		sprintf (name, "%s/%s", dir_name, filename);
/*TODO*///		if( cache_stat (name, &stat_buffer) == 0 )
/*TODO*///			return indx + 1;
/*TODO*///
/*TODO*///		/* try again with a .zip extension */
/*TODO*///		sprintf (name, "%s/%s.zip", dir_name, filename);
/*TODO*///		if( cache_stat (name, &stat_buffer) == 0 )
/*TODO*///			return indx + 1;
/*TODO*///
/*TODO*///		/* try again with a .zif extension */
/*TODO*///		sprintf (name, "%s/%s.zif", dir_name, filename);
/*TODO*///		if( cache_stat (name, &stat_buffer) == 0 )
/*TODO*///			return indx + 1;
/*TODO*///	}
/*TODO*///
/*TODO*///	/* no match */
/*TODO*///	return 0;
/*TODO*///}
/*TODO*///
/*TODO*////* JB 980920 update */
/*TODO*////* AM 980919 update */
    public static Object osd_fopen(String game, String filename, int filetype, int openforwrite) {
        String name = "";
        String gamename;
        int found = 0;
        int indx;
        FakeFileHandle f;
        int pathc = 0;
        String[] pathv = null;

        /*TODO*///	decompose_paths_if_needed();
        f = new FakeFileHandle();
        if (f == null) {
            logerror("osd_fopen: failed to malloc FakeFileHandle!\n");
            return null;
        }
        gamename = game;
        /*TODO*///
/*TODO*///
/*TODO*///	/* Support "-romdir" yuck. */
/*TODO*///	if( alternate_name )
/*TODO*///	{
/*TODO*///		/* check for DEFAULT.CFG file request */
/*TODO*///		if( filetype == OSD_FILETYPE_CONFIG && gamename == "default" )
/*TODO*///		{
/*TODO*///			LOG(("osd_fopen: default input configuration file requested; -romdir switch not applied\n"));
/*TODO*///		} else {
/*TODO*///			LOG(("osd_fopen: -romdir overrides '%s' by '%s'\n", gamename, alternate_name));
/*TODO*/// 			gamename = alternate_name;
/*TODO*///		}
/*TODO*///	}

        switch (filetype) {
            case OSD_FILETYPE_ROM:
            case OSD_FILETYPE_SAMPLE:

                /* only for reading */
                if (openforwrite != 0) {
                    logerror("osd_fopen: type %02x write not supported\n", filetype);
                    break;
                }

                if (filetype == OSD_FILETYPE_SAMPLE) {
                    /*TODO*///			LOG(("osd_fopen: using samplepath\n"));
/*TODO*///			pathc = samplepathc;
/*TODO*///			pathv = samplepathv;
/*HACK*/ pathc = 1;
                    /*HACK*/ pathv = new String[1];
                    /*HACK*/ pathv[0] = "samples";
                } else {
                    /*TODO*///			LOG(("osd_fopen: using rompath\n"));
/*TODO*///			pathc = rompathc;
/*TODO*///			pathv = rompathv;
/*HACK*/ pathc = 1;
                    /*HACK*/ pathv = new String[1];
                    /*HACK*/ pathv[0] = "roms";
                }
                for (indx = 0; indx < pathc && found == 0; ++indx) {
                    String dir_name = pathv[indx];
                    if (found == 0) {
                        name = sprintf("%s/%s", dir_name, gamename);
                        logerror("Trying %s\n", name);
                        osdepend.dlprogress.setFileName("loading file: " + name);
                        if (new File(name).isDirectory() && new File(name).exists()) {
                            name = sprintf("%s/%s/%s", dir_name, gamename, filename);
                            if (new File(name).exists()) {
                                if (filetype == OSD_FILETYPE_ROM) {
                                    f.file = fopen(name, "rb");
                                    long size = ftell(f.file);
                                    f.data = new char[(int) size];
                                    fclose(f.file);
                                    int tlen[] = new int[1];
                                    int tcrc[] = new int[1];
                                    if (checksum_file(name, f.data, tlen, tcrc) == 0) {
                                        f.type = kRAMFile;
                                        f.offset = 0;
                                        found = 1;
                                    }
                                    //copy values where they belong
                                    f.length = tlen[0];
                                    f.crc = tcrc[0];
                                } else {
                                    f.type = kPlainFile;
                                    f.file = fopen(name, "rb");
                                    found = (f.file != null) ? 1 : 0;
                                }
                            }
                        }
                    }
                    if (found == 0) {
                        /* try with a .zip extension */
                        name = sprintf("%s/%s.zip", dir_name, gamename);
                        /*TODO*///				LOG(("Trying %s file\n", name));
                        if (new File(name).exists()) {
                            byte[] bytes = unZipFile(name, filename);
                            if (bytes != null) {
                                if (filetype == OSD_FILETYPE_ROM) {
                                    f.file = fopen(bytes, filename, "rb");
                                    long size = ftell(f.file);
                                    f.data = new char[(int) size];
                                    fclose(f.file);
                                    int tlen[] = new int[1];
                                    int tcrc[] = new int[1];
                                    if (checksum_file_zipped(bytes, filename, f.data, tlen, tcrc) == 0) {
                                        f.type = kZippedFile;
                                        f.offset = 0;
                                        found = 1;
                                    }
                                    //copy values where they belong
                                    f.length = tlen[0];
                                    f.crc = tcrc[0];
                                } else {
                                    f.type = kPlainFile;
                                    f.file = fopen(bytes, filename, "rb");
                                    found = (f.file != null) ? 1 : 0;
                                }
                            }
                        }
                    }
                    if (found == 0) {
                        System.out.println(filename + " does not seem to exist in the zip file");
                        osdepend.dlprogress.setFileName(filename + " does not seem to exist in the zip file");
                    }
                }
                break;

            case OSD_FILETYPE_NVRAM:
                if (found == 0) {
                    name = sprintf("%s/%s.nv", nvdir, gamename);
                    f.type = kPlainFile;
                    f.file = fopen(name, openforwrite != 0 ? "wb" : "rb");
                    found = (f.file != null) ? 1 : 0;
                }
                break;

            case OSD_FILETYPE_HIGHSCORE:
                if (mame_highscore_enabled() != 0) {
                    if (found == 0) {
                        name = sprintf("%s/%s.hi", hidir, gamename);
                        f.type = kPlainFile;
                        f.file = fopen(name, openforwrite != 0 ? "wb" : "rb");
                        found = (f.file != null) ? 1 : 0;
                    }
                }
                break;
            /*TODO*///
/*TODO*///	case OSD_FILETYPE_CONFIG:
/*TODO*///		sprintf (name, "%s/%s.cfg", cfgdir, gamename);
/*TODO*///		f->type = kPlainFile;
/*TODO*///		f->file = fopen (name, openforwrite ? "wb" : "rb");
/*TODO*///		found = f->file != 0;
/*TODO*///
/*TODO*///		break;
/*TODO*///
/*TODO*///	case OSD_FILETYPE_INPUTLOG:
/*TODO*///		sprintf (name, "%s/%s.inp", inpdir, gamename);
/*TODO*///		f->type = kPlainFile;
/*TODO*///		f->file = fopen (name, openforwrite ? "wb" : "rb");
/*TODO*///		found = f->file != 0;
/*TODO*///		if( !openforwrite )
/*TODO*///		{
/*TODO*///			char file[256];
/*TODO*///			sprintf (file, "%s.inp", gamename);
/*TODO*///			sprintf (name, "%s/%s.zip", inpdir, gamename);
/*TODO*///			LOG(("Trying %s in %s\n", file, name));
/*TODO*///			if( cache_stat (name, &stat_buffer) == 0 )
/*TODO*///			{
/*TODO*///				if( load_zipped_file (name, file, &f->data, &f->length) == 0 )
/*TODO*///				{
/*TODO*///					LOG(("Using (osd_fopen) zip file %s for %s\n", name, file));
/*TODO*///					f->type = kZippedFile;
/*TODO*///					f->offset = 0;
/*TODO*///					found = 1;
/*TODO*///				}
/*TODO*///			}
/*TODO*///		}
/*TODO*///
/*TODO*///		break;
/*TODO*///
/*TODO*///	case OSD_FILETYPE_STATE:
/*TODO*///		sprintf (name, "%s/%s.sta", stadir, gamename);
/*TODO*///		f->file = fopen (name, openforwrite ? "wb" : "rb");
/*TODO*///		found = !(f->file == 0);
/*TODO*///		break;

	case OSD_FILETYPE_ARTWORK:
		/* only for reading */
		if( openforwrite != 0)
		{
			logerror("osd_fopen: type %02x write not supported\n",filetype);
			break;
		}
		name = sprintf ("%s/%s", artworkdir, filename);
		f.type = kPlainFile;
		f.file = fopen (name, openforwrite != 0 ? "wb" : "rb");
		found = f.file != null ? 1 : 0;

		if( found == 0 )
		{
			String file, extension;
			file = sprintf("%s", filename);
			name = sprintf("%s/%s", artworkdir, filename);
                        System.out.println(name);
			/*TODO*///extension = strrchr(name, '.');
			/*TODO*///if( extension != null )
			/*TODO*///	strcpy (extension, ".zip");
			/*TODO*///else
			/*TODO*///	strcat (name, ".zip");
			/*TODO*///LOG(("Trying %s in %s\n", file, name));
			/*TODO*///if( cache_stat (name, stat_buffer) == 0 )
			/*TODO*///{
			/*TODO*///	if( load_zipped_file (name, file, f.data, f.length) == 0 )
			/*TODO*///	{
					/*TODO*///LOG(("Using (osd_fopen) zip file %s\n", name));
			/*TODO*///		f.type = kZippedFile;
			/*TODO*///		f.offset = 0;
			/*TODO*///		found = 1;
			/*TODO*///	}
			/*TODO*///}
			if( found == 0 )
			{
				name = sprintf("%s/%s.zip", artworkdir, game);
				/*TODO*///LOG(("Trying %s in %s\n", file, name));
			/*TODO*///	if( cache_stat (name, stat_buffer) == null )
			/*TODO*///	{
			/*TODO*///		if( load_zipped_file (name, file, f.data, f.length) == 0 )
			/*TODO*///		{
			/*TODO*///			/*TODO*///LOG(("Using (osd_fopen) zip file %s\n", name));
			/*TODO*///			f.type = kZippedFile;
			/*TODO*///			f.offset = 0;
			/*TODO*///			found = 1;
			/*TODO*///		}
			/*TODO*///	}
			}
		}
		break;

/*TODO*///	case OSD_FILETYPE_MEMCARD:
/*TODO*///		sprintf (name, "%s/%s", memcarddir, filename);
/*TODO*///		f->type = kPlainFile;
/*TODO*///		f->file = fopen (name, openforwrite ? "wb" : "rb");
/*TODO*///		found = f->file != 0;
/*TODO*///		break;
/*TODO*///
/*TODO*///	case OSD_FILETYPE_SCREENSHOT:
/*TODO*///		/* only for writing */
/*TODO*///		if( !openforwrite )
/*TODO*///		{
/*TODO*///			logerror("osd_fopen: type %02x read not supported\n",filetype);
/*TODO*///			break;
/*TODO*///		}
/*TODO*///
/*TODO*///		sprintf (name, "%s/%s.png", screenshotdir, filename);
/*TODO*///		f->type = kPlainFile;
/*TODO*///		f->file = fopen (name, openforwrite ? "wb" : "rb");
/*TODO*///		found = f->file != 0;
/*TODO*///		break;
/*TODO*///
/*TODO*///	case OSD_FILETYPE_HIGHSCORE_DB:
/*TODO*///		/* only for reading */
/*TODO*///		if( openforwrite )
/*TODO*///		{
/*TODO*///			logerror("osd_fopen: type %02x write not supported\n",filetype);
/*TODO*///			break;
/*TODO*///		}
/*TODO*///		f->type = kPlainFile;
/*TODO*///		/* open as ASCII files, not binary like the others */
/*TODO*///		f->file = fopen (filename, openforwrite ? "w" : "r");
/*TODO*///		found = f->file != 0;
/*TODO*///		break;
/*TODO*///
/*TODO*///	case OSD_FILETYPE_HISTORY:
/*TODO*///		/* only for reading */
/*TODO*///		if( openforwrite )
/*TODO*///		{
/*TODO*///			logerror("osd_fopen: type %02x write not supported\n",filetype);
/*TODO*///			break;
/*TODO*///		}
/*TODO*///		f->type = kPlainFile;
/*TODO*///		/* open as _binary_ like the others */
/*TODO*///		f->file = fopen (filename, openforwrite ? "wb" : "rb");
/*TODO*///		found = f->file != 0;
/*TODO*///		break;
/*TODO*///
/*TODO*///
/*TODO*///	/* Steph */
/*TODO*///	case OSD_FILETYPE_CHEAT:
/*TODO*///		sprintf (name, "%s/%s", cheatdir, filename);
/*TODO*///		f->type = kPlainFile;
/*TODO*///		/* open as ASCII files, not binary like the others */
/*TODO*///		f->file = fopen (filename, openforwrite ? "a" : "r");
/*TODO*///		found = f->file != 0;
/*TODO*///		break;
/*TODO*///
/*TODO*///	case OSD_FILETYPE_LANGUAGE:
/*TODO*///		/* only for reading */
/*TODO*///		if( openforwrite )
/*TODO*///		{
/*TODO*///			logerror("osd_fopen: type %02x write not supported\n",filetype);
/*TODO*///			break;
/*TODO*///		}
/*TODO*///		sprintf (name, "%s.lng", filename);
/*TODO*///		f->type = kPlainFile;
/*TODO*///		/* open as ASCII files, not binary like the others */
/*TODO*///		f->file = fopen (name, openforwrite ? "w" : "r");
/*TODO*///		found = f->file != 0;
/*TODO*///		logerror("fopen %s = %08x\n",name,(int)f->file);
/*TODO*///		break;
            default:
                /*TODO*///		logerror("osd_fopen(): unknown filetype %02x\n",filetype);
                throw new UnsupportedOperationException("Unimplemented");
        }

        if (found == 0) {
            f = null;
            return null;
        }

        return f;
    }

    public static int osd_fread(Object file, char[] buffer, int offset, int length) {
        FakeFileHandle f = (FakeFileHandle) file;

        switch (f.type) {
            case kPlainFile:
                return fread(buffer, offset, 1, length, f.file);
            case kZippedFile:
            case kRAMFile:
                /* reading from the RAM image of a file */
                if (f.data != null) {
                    if (length + f.offset > f.length) {
                        length = f.length - f.offset;
                    }
                    memcpy(buffer, offset, f.data, f.offset, length);
                    f.offset += length;
                    return length;
                }
                break;
        }

        return 0;
    }

    public static int osd_fread(Object file, UBytePtr buffer, int length) {
        return osd_fread(file, buffer.memory, buffer.offset, length);
    }

    public static int osd_fread_lsbfirst(Object file, char[] buffer, int length) {
        return osd_fread(file, buffer, 0, length);
    }

    public static int osd_fread_lsbfirst(Object file, byte[] buffer, int length) {
        char[] buf = new char[length];
        int r = osd_fread(file, buf, 0, length);
        for (int i = 0; i < buf.length; i++) {
            buffer[i] = (byte) buf[i];
        }
        return r;
    }

    public static int osd_fread(Object file, char[] buffer, int length) {
        return osd_fread(file, buffer, 0, length);
    }

    public static int osd_fread(Object file, byte[] buffer, int length) {
        char[] buf = new char[length];
        int r = osd_fread(file, buf, 0, length);
        for (int i = 0; i < buf.length; i++) {
            buffer[i] = (byte) buf[i];
        }
        return r;
    }

    public static int osd_fread(Object file, UBytePtr buffer, int offset, int length) {
        osd_fread(file, buffer.memory, buffer.offset + offset, length);
        return 0;
    }

    /*TODO*///
/*TODO*///int osd_fread_swap (void *file, void *buffer, int length)
/*TODO*///{
/*TODO*///	int i;
/*TODO*///	unsigned char *buf;
/*TODO*///	unsigned char temp;
/*TODO*///	int res;
/*TODO*///
/*TODO*///
/*TODO*///	res = osd_fread (file, buffer, length);
/*TODO*///
/*TODO*///	buf = buffer;
/*TODO*///	for( i = 0; i < length; i += 2 )
/*TODO*///	{
/*TODO*///		temp = buf[i];
/*TODO*///		buf[i] = buf[i + 1];
/*TODO*///		buf[i + 1] = temp;
/*TODO*///	}
/*TODO*///
/*TODO*///	return res;
/*TODO*///}
    public static void osd_fwrite(Object file, UBytePtr buffer, int length) {
        osd_fwrite(file, buffer.memory, buffer.offset, length);
    }

    public static void osd_fwrite(Object file, char[] buffer, int length) {
        osd_fwrite(file, buffer, 0, length);
    }

    public static void osd_fwrite(Object file, UBytePtr buffer, int offset, int length) {
        osd_fwrite(file, buffer.memory, buffer.offset + offset, length);
    }

    public static void osd_fwrite(Object file, char[] buffer, int offset, int length) {
        FakeFileHandle f = (FakeFileHandle) file;

        switch (f.type) {
            case kPlainFile:
                fwrite(buffer, offset, 1, length, f.file);
            default:
                return;
        }
    }

    /*TODO*///int osd_fwrite_swap (void *file, const void *buffer, int length)
/*TODO*///{
/*TODO*///	int i;
/*TODO*///	unsigned char *buf;
/*TODO*///	unsigned char temp;
/*TODO*///	int res;
/*TODO*///
/*TODO*///
/*TODO*///	buf = (unsigned char *) buffer;
/*TODO*///	for( i = 0; i < length; i += 2 )
/*TODO*///	{
/*TODO*///		temp = buf[i];
/*TODO*///		buf[i] = buf[i + 1];
/*TODO*///		buf[i + 1] = temp;
/*TODO*///	}
/*TODO*///
/*TODO*///	res = osd_fwrite (file, buffer, length);
/*TODO*///
/*TODO*///	for( i = 0; i < length; i += 2 )
/*TODO*///	{
/*TODO*///		temp = buf[i];
/*TODO*///		buf[i] = buf[i + 1];
/*TODO*///		buf[i + 1] = temp;
/*TODO*///	}
/*TODO*///
/*TODO*///	return res;
/*TODO*///}
/*TODO*///
/*TODO*///int osd_fread_scatter (void *file, void *buffer, int length, int increment)
/*TODO*///{
/*TODO*///	unsigned char *buf = buffer;
/*TODO*///	FakeFileHandle *f = (FakeFileHandle *) file;
/*TODO*///	unsigned char tempbuf[4096];
/*TODO*///	int totread, r, i;
/*TODO*///
/*TODO*///	switch( f->type )
/*TODO*///	{
/*TODO*///	case kPlainFile:
/*TODO*///		totread = 0;
/*TODO*///		while (length)
/*TODO*///		{
/*TODO*///			r = length;
/*TODO*///			if( r > 4096 )
/*TODO*///				r = 4096;
/*TODO*///			r = fread (tempbuf, 1, r, f->file);
/*TODO*///			if( r == 0 )
/*TODO*///				return totread;		/* error */
/*TODO*///			for( i = 0; i < r; i++ )
/*TODO*///			{
/*TODO*///				*buf = tempbuf[i];
/*TODO*///				buf += increment;
/*TODO*///			}
/*TODO*///			totread += r;
/*TODO*///			length -= r;
/*TODO*///		}
/*TODO*///		return totread;
/*TODO*///		break;
/*TODO*///	case kZippedFile:
/*TODO*///	case kRAMFile:
/*TODO*///		/* reading from the RAM image of a file */
/*TODO*///		if( f->data )
/*TODO*///		{
/*TODO*///			if( length + f->offset > f->length )
/*TODO*///				length = f->length - f->offset;
/*TODO*///			for( i = 0; i < length; i++ )
/*TODO*///			{
/*TODO*///				*buf = f->data[f->offset + i];
/*TODO*///				buf += increment;
/*TODO*///			}
/*TODO*///			f->offset += length;
/*TODO*///			return length;
/*TODO*///		}
/*TODO*///		break;
/*TODO*///	}
/*TODO*///
/*TODO*///	return 0;
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*////* JB 980920 update */
    public static int osd_fseek(Object file, int offset, int whence) {
        FakeFileHandle f = (FakeFileHandle) file;
        int err = 0;

        switch (f.type) {
            case kPlainFile:
                switch (whence) {
                    case SEEK_SET:
                        fseek(f.file, offset, SEEK_SET);
                        return 0;
                    case SEEK_CUR:
                        fseek(f.file, offset, SEEK_CUR);
                        return 0;
                    default:
                        throw new UnsupportedOperationException("FSEEK other than SEEK_SET NOT SUPPORTED.");
                }
            //break;
            case kZippedFile:
            case kRAMFile:
                /* seeking within the RAM image of a file */
                switch (whence) {
                    case SEEK_SET:
                        f.offset = offset;
                        break;
                    case SEEK_CUR:
                        f.offset += offset;
                        break;
                    case SEEK_END:
                        f.offset = f.length + offset;
                        break;
                }
                break;
        }

        return err;
    }

    public static void osd_fclose(Object file) {
        FakeFileHandle f = (FakeFileHandle) file;

        switch (f.type) {
            case kPlainFile:
                fclose(f.file);
                break;
            case kZippedFile:
            case kRAMFile:
                if (f.data != null) {
                    f.data = null;
                }
                break;
        }
        f = null;
    }

    public static int checksum_file(String file, char[] p, int[] size, int[] crc) {
        FILE f;
        f = fopen(file, "rb");
        if (f == null) {
            return -1;
        }

        long length = ftell(f);

        if (fread(p, 1, (int) length, f) != length) {
            fclose(f);
            return -1;
        }
        size[0] = (int) length;
        crc[0] = (int) crc32(p, size[0]);

        return 0;
    }

    /**
     * arcadeflex specific function for checking crc from zipped file
     */
    public static int checksum_file_zipped(byte[] bytes, String filename, char[] p, int[] size, int[] crc) {
        FILE f;
        f = fopen(bytes, filename, "rb");
        if (f == null) {
            return -1;
        }

        long length = ftell(f);

        if (fread(p, 0, 1, (int) length, f) != length) {
            fclose(f);
            return -1;
        }
        size[0] = (int) length;
        crc[0] = (int) crc32(p, size[0]);
        return 0;
    }

    /*TODO*///
/*TODO*////* JB 980920 updated */
/*TODO*////* AM 980919 updated */
/*TODO*///int osd_fchecksum (const char *game, const char *filename, unsigned int *length, unsigned int *sum)
/*TODO*///{
/*TODO*///	char name[256];
/*TODO*///	int indx;
/*TODO*///	struct stat stat_buffer;
/*TODO*///	int found = 0;
/*TODO*///	const char *gamename = game;
/*TODO*///
/*TODO*///	decompose_paths_if_needed();
/*TODO*///
/*TODO*///	/* Support "-romdir" yuck. */
/*TODO*///	if( alternate_name )
/*TODO*///		gamename = alternate_name;
/*TODO*///
/*TODO*///	for( indx = 0; indx < rompathc && !found; ++indx )
/*TODO*///	{
/*TODO*///		const char *dir_name = rompathv[indx];
/*TODO*///
/*TODO*///		if( !found )
/*TODO*///		{
/*TODO*///			sprintf (name, "%s/%s", dir_name, gamename);
/*TODO*///			if( cache_stat (name, &stat_buffer) == 0 && (stat_buffer.st_mode & S_IFDIR) )
/*TODO*///			{
/*TODO*///				sprintf (name, "%s/%s/%s", dir_name, gamename, filename);
/*TODO*///				if( checksum_file (name, 0, length, sum) == 0 )
/*TODO*///				{
/*TODO*///					found = 1;
/*TODO*///				}
/*TODO*///			}
/*TODO*///		}
/*TODO*///
/*TODO*///		if( !found )
/*TODO*///		{
/*TODO*///			/* try with a .zip extension */
/*TODO*///			sprintf (name, "%s/%s.zip", dir_name, gamename);
/*TODO*///			if( cache_stat (name, &stat_buffer) == 0 )
/*TODO*///			{
/*TODO*///				if( checksum_zipped_file (name, filename, length, sum) == 0 )
/*TODO*///				{
/*TODO*///					LOG(("Using (osd_fchecksum) zip file for %s\n", filename));
/*TODO*///					found = 1;
/*TODO*///				}
/*TODO*///			}
/*TODO*///		}
/*TODO*///
/*TODO*///		if( !found )
/*TODO*///		{
/*TODO*///			/* try with a .zif directory (if ZipFolders is installed) */
/*TODO*///			sprintf (name, "%s/%s.zif", dir_name, gamename);
/*TODO*///			if( cache_stat (name, &stat_buffer) == 0 )
/*TODO*///			{
/*TODO*///				sprintf (name, "%s/%s.zif/%s", dir_name, gamename, filename);
/*TODO*///				if( checksum_file (name, 0, length, sum) == 0 )
/*TODO*///				{
/*TODO*///					found = 1;
/*TODO*///				}
/*TODO*///			}
/*TODO*///		}
/*TODO*///	}
/*TODO*///
/*TODO*///	if( !found )
/*TODO*///		return -1;
/*TODO*///
/*TODO*///	return 0;
/*TODO*///}
/*TODO*///
    public static int osd_fsize(Object file) {
        FakeFileHandle f = (FakeFileHandle) file;

        if (f.type == kRAMFile || f.type == kZippedFile) {
            return f.length;
        }

        if (f.file != null) {
            int size = (int) ftell(f.file);
            return size;
        }

        return 0;
    }

    public static int osd_fcrc(Object file) {
        FakeFileHandle f = (FakeFileHandle) file;

        return f.crc;
    }

    /*TODO*///
/*TODO*///int osd_fgetc(void *file)
/*TODO*///{
/*TODO*///	FakeFileHandle *f = (FakeFileHandle *) file;
/*TODO*///
/*TODO*///	if (f->type == kPlainFile && f->file)
/*TODO*///		return fgetc(f->file);
/*TODO*///	else
/*TODO*///		return EOF;
/*TODO*///}
/*TODO*///
/*TODO*///int osd_ungetc(int c, void *file)
/*TODO*///{
/*TODO*///	FakeFileHandle *f = (FakeFileHandle *) file;
/*TODO*///
/*TODO*///	if (f->type == kPlainFile && f->file)
/*TODO*///		return ungetc(c,f->file);
/*TODO*///	else
/*TODO*///		return EOF;
/*TODO*///}
/*TODO*///
/*TODO*///char *osd_fgets(char *s, int n, void *file)
/*TODO*///{
/*TODO*///	FakeFileHandle *f = (FakeFileHandle *) file;
/*TODO*///
/*TODO*///	if (f->type == kPlainFile && f->file)
/*TODO*///		return fgets(s,n,f->file);
/*TODO*///	else
/*TODO*///		return NULL;
/*TODO*///}
/*TODO*///
/*TODO*///int osd_feof(void *file)
/*TODO*///{
/*TODO*///	FakeFileHandle *f = (FakeFileHandle *) file;
/*TODO*///
/*TODO*///	if (f->type == kPlainFile && f->file)
/*TODO*///		return feof(f->file);
/*TODO*///	else
/*TODO*///		return 1;
/*TODO*///}
/*TODO*///
    public static long osd_ftell(Object file) {
        FakeFileHandle f = (FakeFileHandle) file;

        if (f.type == kPlainFile && f.file != null) {
            return ftell(f.file);
        } else {
            return -1L;
        }
    }

    /*TODO*///
/*TODO*///char *osd_basename (char *filename)
/*TODO*///{
/*TODO*///	char *c;
/*TODO*///
/*TODO*///	if (!filename)
/*TODO*///		return NULL;
/*TODO*///
/*TODO*///	c = filename + strlen(filename);
/*TODO*///
/*TODO*///	while (c != filename)
/*TODO*///	{
/*TODO*///		c--;
/*TODO*///		if (*c == '\\' || *c == '/' || *c == ':')
/*TODO*///			return (c+1);
/*TODO*///	}
/*TODO*///
/*TODO*///	return filename;
/*TODO*///}
/*TODO*///
/*TODO*///char *osd_dirname (char *filename)
/*TODO*///{
/*TODO*///	char *dirname;
/*TODO*///	char *c;
/*TODO*///	int found = 0;
/*TODO*///
/*TODO*///	if (!filename)
/*TODO*///		return NULL;
/*TODO*///
/*TODO*///	if ( !( dirname = malloc(strlen(filename)+1) ) )
/*TODO*///	{
/*TODO*///		fprintf(stderr, "error: malloc failed in osd_dirname\n");
/*TODO*///		return 0;
/*TODO*///	}
/*TODO*///
/*TODO*///	strcpy (dirname, filename);
/*TODO*///
/*TODO*///	c = dirname + strlen(dirname);
/*TODO*///	while (c != dirname)
/*TODO*///	{
/*TODO*///		--c;
/*TODO*///		if (*c == '\\' || *c == '/' || *c == ':')
/*TODO*///		{
/*TODO*///			*(c+1)=0;
/*TODO*///			found = 1;
/*TODO*///			break;
/*TODO*///		}
/*TODO*///	}
/*TODO*///
/*TODO*///	/* did we find a path seperator? */
/*TODO*///	if (!found)
/*TODO*///		dirname[0]=0;
/*TODO*///
/*TODO*///	return dirname;
/*TODO*///}
/*TODO*///
/*TODO*///char *osd_strip_extension (char *filename)
/*TODO*///{
/*TODO*///	char *newname;
/*TODO*///	char *c;
/*TODO*///
/*TODO*///	if (!filename)
/*TODO*///		return NULL;
/*TODO*///
/*TODO*///	if ( !( newname = malloc(strlen(filename)+1) ) )
/*TODO*///	{
/*TODO*///		fprintf(stderr, "error: malloc failed in osd_newname\n");
/*TODO*///		return 0;
/*TODO*///	}
/*TODO*///
/*TODO*///	strcpy (newname, filename);
/*TODO*///
/*TODO*///	c = newname + strlen(newname);
/*TODO*///	while (c != newname)
/*TODO*///	{
/*TODO*///		--c;
/*TODO*///		if (*c == '.')
/*TODO*///			*c = 0;
/*TODO*///		if (*c == '\\' || *c == '/' || *c == ':')
/*TODO*///			break;
/*TODO*///	}
/*TODO*///
/*TODO*///	return newname;
/*TODO*///}
    /* called while loading ROMs. It is called a last time with name == 0 to signal */
 /* that the ROM loading process is finished. */
 /* return non-zero to abort loading */
    public static int osd_display_loading_rom_message(String name, int current, int total) {
        if (name != null) {
            System.out.print("loading " + name + "\r\n");
        } else {
            System.out.print("                    \r\n");
        }
//	if( keyboard_pressed (KEYCODE_LCONTROL) && keyboard_pressed (KEYCODE_C) )
//		return 1;
        return 0;
    }
}
