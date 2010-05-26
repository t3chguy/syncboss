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

#define PI_VER2 "v1.0"

#ifdef __alpha
#define PI_VER PI_VER2 " (AXP)"
#else
#define PI_VER PI_VER2 " (x86)"
#endif

#define BUF_SIZE 1048576 /* 1 meg buffer TOO BIG???*/

/* thread prototype */
int killswitch; /* thread killswitch, kill == 1 */
int is_killed; /* has thread killed istelf? yes == 1 */
void dotcpsend(void *kill); 

int getwrittentime();
int getoutputtime();

int srate, numchan, bps, active;
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
char header[32];
int has_sent_header;

BOOL WINAPI _DllMainCRTStartup(HANDLE hInst, ULONG ul_reason_for_call, LPVOID lpReserved)
{
	return TRUE;
}

Out_Module out;

int canwrite();

void config(HWND hwnd)
{
	char ansistr[20];
	int a;
	BSTR unicodestr;

	sprintf(ansistr, "%d", canwrite());
	a = lstrlenA(ansistr);
	unicodestr = SysAllocStringLen(NULL, a);
	MultiByteToWideChar(CP_ACP, 0, ansistr, a, unicodestr, a);
	/*MessageBox(out.hMainWindow, unicodestr, L"", MB_OK); *//* todo: remove debugger code */
	SysFreeString(unicodestr);
}

void about(HWND hwnd)
{
}

void initsocket();

void init()
{
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
	sprintf(ansistr, "samplerate: %d, numchans: %d, bits/sample: %d", samplerate, numchannels, bitspersamp);
	a = lstrlenA(ansistr);
	unicodestr = SysAllocStringLen(NULL, a);
	MultiByteToWideChar(CP_ACP, 0, ansistr, a, unicodestr, a);
	/*MessageBox(out.hMainWindow, unicodestr, L"", MB_OK); *//* todo: remove debugger code */
	SysFreeString(unicodestr);
	

	/* end debugging stuff */

	start_t=GetTickCount();
 	w_offset = 0;
	active=1;
	numchan = numchannels;
	srate = samplerate;
	bps = bitspersamp;

	/* build header */
	has_sent_header = 0;	
	*(header + 0) = 0x01;
	*(header + 4) = (char)((samplerate) & 0x000000FF);
	*(header + 3) = (char)((samplerate >> 8) & 0x000000FF);
	*(header + 2) = (char)((samplerate >> 16) & 0x000000FF);
	*(header + 1) = (char)((samplerate >> 24) & 0x000000FF);

	*(header + 5) = (char)numchannels;
	*(header + 6) = (char)bitspersamp;

	
	/* open socket */

	/*MessageBox(out.hMainWindow, L"Open code getting cald.", L"", MB_OK);*/
	if(!isopen) {
		if (connect(sock,(struct sockaddr *) &address, sizeof(address)) != 0) {
			failed=1;
			/*MessageBox(out.hMainWindow, L"Connection to SyncBoss server failed.", L"", MB_OK);*/
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

int numbytes=0;
char silence[1024] = {0};
int send_result=0;

int mainheader() {
	int headercount = 0;
	do {
		send_result = send(sock,header+sizeof(header)*headercount,32-headercount,0);
		if(send_result < 0) {
			return -1;
		}
		headercount += send_result;
	} while(headercount < 32);
	has_sent_header = 1;
	return 1;
}

int doheaders() {
	if(numbytes==1024) {
		numbytes = 0;
	}
	if(!has_sent_header && numbytes != 0) { //silence
		do {
			send_result = send(sock,silence,1024-numbytes,0);
			if(send_result < 0) {
				return -1;
			}
			numbytes += send_result;
		} while(numbytes < 1024);
		numbytes = 0;
		if(mainheader()<0) return -1;
	} else if(!has_sent_header && numbytes == 0) {
		if(mainheader()<0) return -1;
	} else if(has_sent_header && numbytes == 0) {
		do {
			send_result = send(sock,silence,1,0);
			if(send_result < 0) {
				return -1;
			}			
		} while(send_result == 0);
	}	
	return 1;

}

void dotcpsend(void *kill) {
	int send_len;
	int writehead;
	is_killed = 0;
	if(!failed) {
		while(!*((int*)kill)) {
			if(!has_sent_header || (numbytes == 1024) || (numbytes == 0)) {
				if(doheaders()<0) { failed=1; isopen=0; writel=read; initsocket();numbytes=0; break; }
			}
			writehead = (int)(writel % BUF_SIZE);
			send_len = min(min(read - writel,BUF_SIZE - writehead), 1024-numbytes); /* make sure we only send contiguous data */
			if(send_len>0) {
				send_result = send(sock,buf + sizeof(char) * writehead,send_len,0);
				if(send_result < 0) {
					//error
					isopen = 0;
					failed = 1;
					writel=read;
					/*MessageBox(out.hMainWindow, L"Message to SyncBoss server failed.", L"", MB_OK);*/
					initsocket();
					numbytes=0;
					break;
				}
				writel = writel + send_result;
				numbytes = numbytes + send_result;
			} 
			/* thread sleep here */
			/*Sleep(1);*/
		}
		
		
	}
	/* thread kill here */
	/*MessageBox(out.hMainWindow, L"Write thread ended.", L"", MB_OK); *//* todo: remove debugger code */
	is_killed = 1;
	_endthread();
}

int write(char *inbuf, int len)
{
	int i;
	
	if(len > canwrite())
		return 1;

	for(i=0;i<len;i++) {
		buf[(int)(read % BUF_SIZE)] = inbuf[i];
		read++;
	}
	return 0;
}

int canwrite()
{	

	if (failed) return 0;
	return min(BUF_SIZE-(read-writel), 65536);
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
	int bitsps = srate * bps * numchan; /* bits per second */
	int bytesps = bitsps / 8; /* bytes per second */
	double bytespms = ((double)bytesps) / 1000.0;/* bytes per millisecond */
	return max(0,(int)((double)writel/bytespms)-1000); /* return output time in milliseconds... 3000 is to do with prebuffer to synced players */
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
	int bitsps = srate * bps * numchan; /* bits per second */
	int bytesps = bitsps / 8; /* bytes per second */
	double bytespms = ((double)bytesps) / 1000.0;/* bytes per millisecond */
	return (int)((double)read/bytespms); /* return output time in milliseconds... */
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
