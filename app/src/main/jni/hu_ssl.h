#ifndef HEADUNIT_HU_SSL_H
#define HEADUNIT_HU_SSL_H

#include <openssl/ssl.h>
#include <openssl/bio.h>
#include <openssl/ossl_typ.h>

#define byte unsigned char

extern SSL *hu_ssl_ssl;//  = NULL;
extern BIO *hu_ssl_rm_bio;//  = NULL;
extern BIO *hu_ssl_wm_bio;//  = NULL;

void hu_ssl_ret_log(int ret);
void hu_ssl_inf_log();

int hu_ssl_do_handshake();
int hu_ssl_prepare();
int hu_ssl_bio_read(int offset, int res_max, byte *res_buf);
int hu_ssl_bio_write(int offset, int msg_len, byte *msg_buf);
int hu_ssl_read(int offset, int res_max, byte *res_buf);
int hu_ssl_write(int offset, int msg_len, byte *msg_buf);

#endif //HEADUNIT_HU_SSL_H
