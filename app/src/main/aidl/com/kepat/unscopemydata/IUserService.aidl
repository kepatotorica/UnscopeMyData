package com.kepat.unscopemydata;

interface IUserService {
    void destroy() = 16777114;
    boolean executeCommand(String command) = 1;
    List<String> listFiles(String path) = 2;
    boolean isDirectory(String path) = 3;
    boolean deleteDirectory(String path) = 4;
}
