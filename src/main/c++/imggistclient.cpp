#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h> // struct sockaddr_in
#include <arpa/inet.h> // inet_addr()
#include <unistd.h> // close() deallocate the file description
#include "ImgGistClient.h"

int ImgGistClient::CreateSocket(void) {
  sockfd_ = socket(AF_INET, SOCK_STREAM, 0);
  if (-1 != sockfd_) {
    return 0;
  }
  return -1;
}

int ImgGistClient::ConnectTo(const char* host, const int& port) {
  if (-1 == sockfd_) {
    return -1;
  }

  struct sockaddr_in svr_addr;
  //memset(&svr_addr, 0, sizeof(svr_addr));
  ServerAddress(host, port, &svr_addr);

  return connect(sockfd_, (struct sockaddr*)&svr_addr, sizeof(svr_addr));
}

int ImgGistClient::Request(const char* req, const int& req_size, char* resp_buf, const int& resp_buf_size, int* resp_size) {
  if (-1 == sockfd_) {
    return -1;
  }

  int send_size = send(sockfd_, req, req_size, 0);
  shutdown(sockfd_, SHUT_WR);

  if (0 < send_size) {
    *resp_size = recv(sockfd_, resp_buf, resp_buf_size, 0);
    shutdown(sockfd_, SHUT_RD);
    if (0 >= *resp_size) return -3; // receive fail
  } else {
    return -2; // send fail
  }

  return 0;
}

int ImgGistClient::ServerAddress(const char* host, const int& port, struct sockaddr_in* svr_addr) {
  svr_addr->sin_family = AF_INET;
  svr_addr->sin_addr.s_addr = inet_addr(host);
  svr_addr->sin_port = htons(port);

  return 0;
}

int ImgGistClient::Close(void) {
  if (-1 != sockfd_) {
    return close(sockfd_);
  }

  return -1;
}