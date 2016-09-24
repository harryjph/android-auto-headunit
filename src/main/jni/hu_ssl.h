#ifndef HEADUNIT_HU_SSL_H
#define HEADUNIT_HU_SSL_H

#include "libs/headers/openssl/ssl.h"
#include "libs/headers/openssl/bio.h"
#include "libs/headers/openssl/ossl_typ.h"

extern SSL *hu_ssl_ssl;//  = NULL;
extern BIO *hu_ssl_rm_bio;//  = NULL;
extern BIO *hu_ssl_wm_bio;//  = NULL;

void hu_ssl_ret_log(int ret);
void hu_ssl_inf_log();
int hu_ssl_handshake();

#endif //HEADUNIT_HU_SSL_H
