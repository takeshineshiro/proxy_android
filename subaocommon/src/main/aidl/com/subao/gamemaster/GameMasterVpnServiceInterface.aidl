package com.subao.gamemaster;

interface GameMasterVpnServiceInterface {

    int startProxy(in List<String> allowedPackageNames);

    void stopProxy();

    boolean protectSocket(int socket);

}
