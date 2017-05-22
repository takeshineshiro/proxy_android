#include <sys/socket.h>
#include <sys/un.h>
#include <stdlib.h>
#include "cn_wsds_gamemaster_tools_RootUtil.h"

// 和自定义su约定，不要乱改！！！
#define SOCKET_NAME "com.subao.su.socket"

// 检测守护进程是否正在运行
JNIEXPORT jboolean JNICALL Java_cn_wsds_gamemaster_tools_RootUtil_isDaemonRunning(JNIEnv *env, jobject obj) {
	int ret = 0;

	int fd = socket(AF_LOCAL, SOCK_STREAM, 0);
	if (fd < 0) {
		return 0;
	}

	struct sockaddr_un sun;
	memset(&sun, 0, sizeof(sun));
	sun.sun_family = AF_LOCAL;
	memcpy(sun.sun_path, "\0" SOCKET_NAME, strlen(SOCKET_NAME) + 1);

	if (connect(fd, (struct sockaddr*) &sun, sizeof(sun)) == 0) {
		ret = 1; // 运行中
	} else {
		ret = 0; // 没运行
	}

	close(fd);
	return ret;
}
