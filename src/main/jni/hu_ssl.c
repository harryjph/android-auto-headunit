#include "hu_uti.h"

#include "libs/headers/openssl/bio.h"
#include "libs/headers/openssl/ssl.h"
#include "libs/headers/openssl/rand.h"
#include "hu_ssl.h"
#include "hu_ssl_cert.h"

SSL_METHOD *hu_ssl_method = NULL;
SSL_CTX *hu_ssl_ctx = NULL;
SSL *hu_ssl_ssl = NULL;
BIO *hu_ssl_rm_bio = NULL;
BIO *hu_ssl_wm_bio = NULL;

void hu_ssl_inf_log() {

    const char *ssl_state_string_long = SSL_state_string_long(hu_ssl_ssl);   // "SSLv3 write client hello B"
    logd ("ssl_state_string_long: %s", ssl_state_string_long);

    const char *ssl_version = SSL_get_version(hu_ssl_ssl);                   // "TLSv1.2"
    logd ("ssl_version: %s", ssl_version);

    const SSL_CIPHER *ssl_cipher = SSL_get_current_cipher(hu_ssl_ssl);
    const char *ssl_cipher_name = SSL_CIPHER_get_name(ssl_cipher);    // "(NONE)"
    logd ("ssl_cipher_name: %s", ssl_cipher_name);
}

void hu_ssl_ret_log(int ret) {
    int ssl_err = SSL_get_error(hu_ssl_ssl, ret);
    char *err_str;

    switch (ssl_err) {
        case SSL_ERROR_NONE:
            err_str = "";
            break;
        case SSL_ERROR_ZERO_RETURN:
            err_str = "SSL_ERROR_ZERO_RETURN";
            break;
        case SSL_ERROR_WANT_READ:
            err_str = "SSL_ERROR_WANT_READ";
            break;
        case SSL_ERROR_WANT_WRITE:
            err_str = "SSL_ERROR_WANT_WRITE";
            break;
        case SSL_ERROR_WANT_CONNECT:
            err_str = "SSL_ERROR_WANT_CONNECT";
            break;
        case SSL_ERROR_WANT_ACCEPT:
            err_str = "SSL_ERROR_WANT_ACCEPT";
            break;
        case SSL_ERROR_WANT_X509_LOOKUP:
            err_str = "SSL_ERROR_WANT_X509_LOOKUP";
            break;
        case SSL_ERROR_SYSCALL:
            err_str = "SSL_ERROR_SYSCALL";
            break;
        case SSL_ERROR_SSL:
            err_str = "SSL_ERROR_SSL";
            break;
        default:
            err_str = "SSL_ERROR_UNKNOWN";
            break;
    }

    if (strlen(err_str) == 0)
        logd ("ret: %d  ssl_err: %d (Success)", ret, ssl_err);
    else
        loge ("ret: %d  ssl_err: %d (%s)", ret, ssl_err, err_str);
}

int hu_ssl_prepare()
{
    int ret;
    BIO * cert_bio = NULL;
    BIO * pkey_bio = NULL;
    ret = SSL_library_init();                                          // Init
    logd ("SSL_library_init ret: %d", ret);
    if (ret != 1) {                                                     // Always returns "1", so it is safe to discard the return value.
        loge ("SSL_library_init() error");
        return -1;
    }

#ifdef __arm__
    OPENSSL_add_all_algorithms_noconf();                               // Add all algorithms, without using config file
#endif

    ret = RAND_status();                                               // 1 if the PRNG has been seeded with enough data, 0 otherwise.
    logd ("RAND_status ret: %d", ret);
    if (ret != 1) {
        loge ("RAND_status() error");
        return -1;
    }

    cert_bio = BIO_new_mem_buf(cert_buf, sizeof(cert_buf));           // Read only memory BIO for certificate
    pem_password_cb * ppcb1 = NULL;
    void *u1 = NULL;
    X509 * x509 = NULL;
    X509 * x509_cert = PEM_read_bio_X509_AUX(cert_bio, &x509, ppcb1, u1);
    if (x509_cert == NULL) {
        loge ("read_bio_X509_AUX() error");
        return -1;
    }
    logd ("PEM_read_bio_X509_AUX() x509_cert: %p", x509_cert);
    ret = BIO_free(cert_bio);
    if (ret != 1)
        loge ("BIO_free(cert_bio) ret: %d", ret);
    else
        logd ("BIO_free(cert_bio) ret: %d", ret);

    pkey_bio = BIO_new_mem_buf(pkey_buf, sizeof(pkey_buf));           // Read only memory BIO for private key
    pem_password_cb * ppcb2 = NULL;
    void *u2 = NULL;
    // Read a private key from a BIO using a pass phrase callback:    key = PEM_read_bio_PrivateKey(bp, NULL, pass_cb,  "My Private Key");
    // Read a private key from a BIO using the pass phrase "hello":   key = PEM_read_bio_PrivateKey(bp, NULL, 0,        "hello");
    EVP_PKEY * priv_key_ret = NULL;
    EVP_PKEY * priv_key = PEM_read_bio_PrivateKey(pkey_bio, &priv_key_ret, ppcb2, u2);
    if (priv_key == NULL) {
        loge ("PEM_read_bio_PrivateKey() error");
        return -1;
    }
    logd ("PEM_read_bio_PrivateKey() priv_key: %p", priv_key);
    ret = BIO_free(pkey_bio);
    if (ret != 1)
        loge ("BIO_free(pkey_bio) ret: %d", ret);
    else
        logd ("BIO_free(pkey_bio) ret: %d", ret);

    hu_ssl_method = (SSL_METHOD *) TLSv1_2_client_method();
    if (hu_ssl_method == NULL) {
        loge ("TLSv1_2_client_method() error");
        return -1;
    }
    logd ("TLSv1_2_client_method() hu_ssl_method: %p", hu_ssl_method);

    hu_ssl_ctx = SSL_CTX_new(hu_ssl_method);
    if (hu_ssl_ctx == NULL) {
        loge ("SSL_CTX_new() error");
        return -1;
    }
    logd ("SSL_CTX_new() hu_ssl_ctx: %p", hu_ssl_ctx);

    ret = SSL_CTX_use_certificate(hu_ssl_ctx, x509_cert);
    if (ret != 1)
        loge ("SSL_CTX_use_certificate() ret: %d", ret);
    else
        logd ("SSL_CTX_use_certificate() ret: %d", ret);

    ret = SSL_CTX_use_PrivateKey(hu_ssl_ctx, priv_key);
    if (ret != 1)
        loge ("SSL_CTX_use_PrivateKey() ret: %d", ret);
    else
        logd ("SSL_CTX_use_PrivateKey() ret: %d", ret);


    // Must do all CTX setup before SSL_new() !!
    hu_ssl_ssl = SSL_new(hu_ssl_ctx);
    if (hu_ssl_ssl == NULL) {
        loge ("SSL_new() hu_ssl_ssl: %p", hu_ssl_ssl);
        return -1;
    }
    logd ("SSL_new() hu_ssl_ssl: %p", hu_ssl_ssl);

    ret = SSL_check_private_key(hu_ssl_ssl);
    if (ret != 1) {
        loge ("SSL_check_private_key() ret: %d", ret);
        return -1;
    }
    logd ("SSL_check_private_key() ret: %d", ret);


    hu_ssl_rm_bio = BIO_new(BIO_s_mem());
    if (hu_ssl_rm_bio == NULL) {
        loge ("BIO_new() hu_ssl_rm_bio: %p", hu_ssl_rm_bio);
        return -1;
    }
    logd ("BIO_new() hu_ssl_rm_bio: %p", hu_ssl_rm_bio);

    hu_ssl_wm_bio = BIO_new(BIO_s_mem());
    if (hu_ssl_wm_bio == NULL) {
        loge ("BIO_new() hu_ssl_wm_bio: %p", hu_ssl_wm_bio);
        return -1;
    }
    logd ("BIO_new() hu_ssl_wm_bio: %p", hu_ssl_wm_bio);

    SSL_set_bio(hu_ssl_ssl, hu_ssl_rm_bio, hu_ssl_wm_bio);

    BIO_set_write_buf_size (hu_ssl_rm_bio, DEFBUF);
    BIO_set_write_buf_size (hu_ssl_wm_bio, DEFBUF);

    SSL_set_connect_state(hu_ssl_ssl);                                        // Set ssl to work in client mode

    SSL_set_verify(hu_ssl_ssl, SSL_VERIFY_NONE, NULL);
    return 0;
}

int hu_ssl_do_handshake()
{
    int ret = SSL_do_handshake(hu_ssl_ssl);                             // Do current handshake step processing
    logd ("hu_ssl_handshake ret: %d, error: %d", ret, SSL_get_error(hu_ssl_ssl, ret));

    if (SSL_get_error(hu_ssl_ssl, ret) != SSL_ERROR_WANT_READ) {
        hu_ssl_ret_log(ret);
        hu_ssl_inf_log();
    }
    return 0;
}

int hu_ssl_bio_read(int offset, int res_max, byte *res_buf)
{
    int ret = BIO_read(hu_ssl_wm_bio, &res_buf[offset], res_max);
    // Read from the BIO Client request: Hello/Key Exchange
    if (ret <= 0) {
        loge ("BIO_read read ret: %d", ret);
        return -1;
    }
    return ret;
}

int hu_ssl_bio_write(int offset, int msg_len, byte *msg_buf)
{
    return BIO_write(hu_ssl_rm_bio, &msg_buf[offset], msg_len);
}

int hu_ssl_read(int offset, int res_max, byte *res_buf)
{
    int ret = SSL_read(hu_ssl_ssl, &res_buf[offset], res_max);
    if (ret < 0)
    {
       loge ("SSL_read() result: %d", ret);
       hu_ssl_ret_log(ret);
    }
    return ret;
}

int hu_ssl_write(int offset, int msg_len, byte *msg_buf)
{
    return SSL_write(hu_ssl_ssl, &msg_buf[offset], msg_len);
}
