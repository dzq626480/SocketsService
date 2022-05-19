#pragma once

class ImgGistClient {
  public:
    int CreateSocket(void);
    int ConnectTo(const char* host, const int& port);
    int Request(const char* req, const int& req_size, char* resp_buf, const int& resp_buf_size, int* resp_size);
    int Close(void);
  private:
    int sockfd_ = -1;

    int ServerAddress(const char* host, const int& port, struct sockaddr_in* svr_addr);
};
