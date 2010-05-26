#include <windows.h>
#include <winsock.h>
#include <stdlib.h>
#include <stdio.h>
#include <shlobj.h>
#include <errno.h>      // Needed for errno
#include <stddef.h>     // Needed for _threadid
#include <process.h>    // Needed for _beginthread(), and _endthread()
#include <math.h>
#include "../winamp/out.h"

#define PI_VER2 "v1.01"

#ifdef __alpha
#define PI_VER PI_VER2 " (AXP)"
#else
#define PI_VER PI_VER2 " (x86)"
#endif

#define BUF_SIZE 1048576 /* 1 meg buffer TOO BIG???*/
#define PACKET_SIZE 1024

/* thread prototype */
int killswitch; /* thread killswitch, kill == 1 */
int is_killed; /* has thread killed istelf? yes == 1 */
/*unsigned int thread_id;*/
void dotcpsend(void *kill); 



int getwrittentime();
int getoutputtime();

int srate, numchan, bps, active;
int dupe;
volatile int w_offset;


WORD wVersionRequested;          /* socket dll version info */ 
WSADATA wsaData;                 /* data for socket lib initialisation */
SOCKET sock;                     /* socket details */
struct sockaddr_in address;      /* socket address stuff */
struct hostent * host;           /* host stuff */
char HostName[100];              /* host name from user */
long iMode;						 /* 0: blocking, 1: noblocking */
int failed;
char buf[BUF_SIZE];
long read;
long writel;
int isopen;
int has_sent_header;
char header[32];				/* header[0] : always 1, header[1] - header[4] : samplerate, header[5] : numchannels, header[6] : bitspersample, header[7]-header[32] : unused */
char* no_header_trigger;
char* silence;
int bytes_since_header=0;
char* sb_source;
int send_result;
FILE* megalog;

BOOL WINAPI _DllMainCRTStartup(HANDLE hInst, ULONG ul_reason_for_call, LPVOID lpReserved)
{
	return TRUE;
}

Out_Module out;

int canwrite();

void config(HWND hwnd)
{
	char ansistr[69];
	int a;
	BSTR unicodestr;

	sprintf(ansistr, "%d.read:%d.writel:%d", canwrite(), read, writel);
	a = lstrlenA(ansistr);
	unicodestr = SysAllocStringLen(NULL, a);
	MultiByteToWideChar(CP_ACP, 0, ansistr, a, unicodestr, a);
	MessageBox(out.hMainWindow, unicodestr, L"", MB_OK);/* todo: remove debugger code */
	SysFreeString(unicodestr);
}

void about(HWND hwnd)
{
}

void initsocket();

void init()
{
	int i;
	//header = (char*)malloc(sizeof(char)*32);
	no_header_trigger = (char*)malloc(sizeof(char));
	silence = (char*)malloc(sizeof(char)*PACKET_SIZE);
	sb_source = (char*)malloc(sizeof(char*));
	for(i=0;i<32;i++) {
		*(header + i) = i;
	}
	for(i=0;i<PACKET_SIZE;i++) {
		*(silence + i) = 0;
	}
	*no_header_trigger = 0;

	failed = 0;
	read = 0;
	writel = 0;
	isopen = 0;
	killswitch = 0;
	is_killed = 1;

	/*MessageBox(out.hMainWindow, L"SyncBoss plugin initialising.", L"", MB_OK);*/
	strcat(HostName, "127.0.0.1");

	wVersionRequested = MAKEWORD( 1, 1 );
	WSAStartup( wVersionRequested, &wsaData );
	initsocket();

	iMode = 1;				/* disable blocking */
	/*ioctlsocket(sock, FIONBIO, &iMode);*/

	address.sin_family=AF_INET;       /* internet */
	address.sin_port = htons(51923);     /* port 51923 for syncboss input */

	host=gethostbyname(HostName);

	address.sin_addr.s_addr=*((unsigned long *) host->h_addr);

	/* debug... open file */
	megalog = fopen("C:\\syncboss_out_log.txt", "w");
}

void initsocket() {
	sock = socket(AF_INET, SOCK_STREAM, 0);
	/*MessageBox(out.hMainWindow, L"re-init socket.", L"", MB_OK);*/
}

void quit()
{
	closesocket(sock);
	WSACleanup(); /* clean up before exit */
}

int start_t;

int open(int samplerate, int numchannels, int bitspersamp, int bufferlenms, int prebufferms)
{
	/*debugging stuff */
	char ansistr[169];
	int a;
	BSTR unicodestr;
	/*sprintf(ansistr, "samplerate: %d, numchans: %d, bits/sample: %d", samplerate, numchannels, bitspersamp);*/
	a = lstrlenA(ansistr);
	unicodestr = SysAllocStringLen(NULL, a);
	MultiByteToWideChar(CP_ACP, 0, ansistr, a, unicodestr, a);
	/*MessageBox(out.hMainWindow, unicodestr, L"", MB_OK); *//* todo: remove debugger code */
	SysFreeString(unicodestr);	
	/* end debugging stuff */

	if(samplerate == 0 || numchannels == 0 || bitspersamp == 0) {
		return -1;
	}

	/* build header */
	has_sent_header = 0;	
	*(header + 0) = 1;
	*(header + 4) = (char)((samplerate) & 0x000000FF);
	*(header + 3) = (char)((samplerate >> 8) & 0x000000FF);
	*(header + 2) = (char)((samplerate >> 16) & 0x000000FF);
	*(header + 1) = (char)((samplerate >> 24) & 0x000000FF);

	*(header + 5) = (char)numchannels;
	*(header + 6) = (char)bitspersamp;


	start_t=GetTickCount();
 	w_offset = 0;
	active=1;
	numchan = numchannels;
	srate = samplerate;
	bps = bitspersamp;

	/* frame dupe num: */
	/*dupe = 1;
	dupe = dupe * (2 / numchan);
	dupe = dupe * (44100 / srate);
	dupe = dupe * (16 / bps);*/

	/* flush counters */
	read = 0;
	writel = 0;
	
	/* open socket */

	/*MessageBox(out.hMainWindow, L"Open code getting cald.", L"", MB_OK);*/
	if(!isopen) {
		initsocket();
		if (sock!=INVALID_SOCKET && connect(sock,(struct sockaddr *) &address, sizeof(address)) != 0) {
			failed=1;
			/*MessageBox(out.hMainWindow, L"WINAMP PLUGIN: Connection to SyncBoss server failed; make sure the server is in DJ mode.", L"", MB_OK);*/
			return -1;
		} else {
			/*MessageBox(out.hMainWindow, L"Connection Succeeded.", L"", MB_OK); *//* todo: remove debug */
			failed=0;
		}
	}

	isopen = 1;
	
	killswitch = 1;
	while(!is_killed) {
		Sleep(1);
	}
	killswitch = 0;
	/* start thread here */
	_beginthread(dotcpsend,8192,(void *)&killswitch);

	return 2500;
}

void close()
{
	killswitch = 1;

}

void dotcpsend(void *kill) {
	int send_len;
	int i;
	int start;
	int is_printing_header = 0;
	int megadebugcount = 0;

	is_killed = 0;
	if(!failed) {
		while(!*((int*)kill)) {
			if(!has_sent_header) { /*need to update header*/
				if(bytes_since_header != PACKET_SIZE && bytes_since_header != 0) { /* not time for new header, send some silence */
					sb_source = silence;
					start = 0;
					send_len = PACKET_SIZE - bytes_since_header;
				} else { /* send actual header */
					sb_source = header;
					start = 0;
					send_len = 32;
					has_sent_header = 1;
					is_printing_header = 1;
				}				
			} else {
				if(bytes_since_header == PACKET_SIZE) { /* inform syncboss that no header is coming */
					sb_source = no_header_trigger;
					start = 0;
					send_len = 1;
					is_printing_header = 1;
					bytes_since_header = 0;
					megadebugcount++;
				} else {
					sb_source = buf;
					start = (int)(writel % BUF_SIZE);
					send_len = min(min(read - writel,BUF_SIZE - start), PACKET_SIZE-bytes_since_header); /* make sure we only send contiguous data, and no more than remaining in 1 packet */
				}
			}
			if(send_len>0) {
				do {
					send_result = send(sock,sb_source + sizeof(char) * start,send_len,0);
					if(send_result < 0) {
						//error
						bytes_since_header = 0;
						isopen = 0;
						failed = 1;
						writel=read;
						sock = INVALID_SOCKET;
						goto kill;
						/*MessageBox(out.hMainWindow, L"Message to SyncBoss server failed.", L"", MB_OK);*/				
					}
					/*debug*/
					for(i=0;i<send_result;i++) {
						fprintf(megalog, "%d\n", (int)*(sb_source+sizeof(char)*(start+i)));
					}
					/*end debug*/
					writel = writel + send_result;
					bytes_since_header += send_result;
					send_len -= send_result;
					start += send_result;
				} while (send_len > 0 && has_sent_header);
				
			}

			if(is_printing_header == 1) {
				fprintf(megalog, "Sent header (%d)\n", bytes_since_header); 
				is_printing_header = 0;
				bytes_since_header = 0;
			} else if(sb_source==buf) {
				fprintf(megalog, "Sent some data %d (%d)\n",megadebugcount, bytes_since_header);
			} else if(sb_source==silence) {
				fprintf(megalog, "Sent some silence (%d)\n", bytes_since_header);
			}
			/* thread sleep here */
			/*Sleep(1);*/
		}
	}
	/* thread kill here */
	/*MessageBox(out.hMainWindow, L"Write thread ended.", L"", MB_OK); *//* todo: remove debugger code */
kill:
	is_killed = 1;
	_endthread();
}

/* todo: transmit format, rather than upsampling */
int write(char *inbuf, int len)
{
	int i;
	/*int j;*/
	int k;
	
	if(len > canwrite())
		return 1;

	for(i=0;i<len;i+=bps) { /*iterate through the frames (aka samples)*/
		/*for(j=0;j<dupe;j++) { *//*dupe it however many times to match 44.1khz / 32 bps / 2 channels*/
			for(k=0;k<bps;k++) { /*copy the actual frame //todo: might be more efficient to memcpy*/
				buf[(int)(read % BUF_SIZE)] = inbuf[i+k];
				read++;
			}
		/*}*/
	}
	return 0;
}

int canwrite()
{	

	if (failed) return 0;
	return min(BUF_SIZE-(read-writel), 65536)/*/dupe*/;
	/*if (last_pause) return 0;
	if (getwrittentime() < getoutputtime()+MulDiv(65536,1000,srate*bps*numchan/8)) return 65536;
	return 0;*/
}

int isplaying()
{
	if (read>writel) return 1;
	return 0;
}

int pause(int pause)
{
	/*int t=last_pause;
	if (!last_pause && pause) { w_offset+=GetTickCount()-start_t;  writtentime=0; }
	if (last_pause && !pause) { start_t=GetTickCount(); }
	last_pause=pause;*/
	return 0;
}

void setvolume(int volume)
{
}

void setpan(int pan)
{
}

void flush(int t)
{
  /*w_offset=t;
  start_t=GetTickCount()+5000;
  writtentime=0;*/
	read=0;
	writel=0;
	/*killswitch = 1;*/
}
	
int getoutputtime()
{
	/*if (last_pause)
		return w_offset;*/
	/*return GetTickCount()-start_t;*//* + w_offset;*/
	/* todo: cleanup this */
	int bitsps = srate * bps * numchan /** dupe*/; /* bits per second */
	int bytesps = bitsps / 8; /* bytes per second */
	double bytespms = ((double)bytesps) / 1000.0;/* bytes per millisecond */
	if(bitsps == 0) {
		return 0;
	}
	return max(0,(int)((double)(writel)/bytespms)-1000); /* return output time in milliseconds... 3000 is to do with prebuffer to synced players */
}

int getwrittentime()
{/*
	int t=srate*numchan,l;
	int ms=writtentime;

	if (t)
	{
	l=ms%t;
	ms /= t;
	ms *= 1000;
	ms += (l*1000)/t;

	ms/=(bps/8);

	return ms ;*//*+ w_offset;*//*
	}
	else*/
/*		return ms;*/
	/* todo: cleanup this */	
	int bitsps = srate * bps * numchan /** dupe*/; /* bits per second */
	int bytesps = bitsps / 8; /* bytes per second */
	double bytespms = ((double)bytesps) / 1000.0;/* bytes per millisecond */
	if(bitsps == 0) {
		return 0;
	}
	return (int)((double)(read)/bytespms); /* return output time in milliseconds... */
}

Out_Module out = {
	OUT_VER,
	"SyncBoss server output plugin " PI_VER,
	6969,
	0, // hmainwindow
	0, // hdllinstance
	config,
	about,
	init,
	quit,
	open,
	close,
	write,
	canwrite,
	isplaying,
	pause,
	setvolume,
	setpan,
	flush,
	getoutputtime,
	getwrittentime
};

__declspec( dllexport ) Out_Module * winampGetOutModule()
{
	return &out;
}
