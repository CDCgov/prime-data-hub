package gov.cdc.prime.router.tokens

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.kittinunf.fuel.util.decodeBase64
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jose.jwk.KeyType
import java.math.BigInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class JwkTests {
    val pemPublicESKey = """
-----BEGIN PUBLIC KEY-----
MHYwEAYHKoZIzj0CAQYFK4EEACIDYgAE0ks/RiTQ82tMW4UEQu0LLDqqEEcj6yce
ZF5YuUU+IqOKaMAu4/tsbyE+hM4WDjZYG6cSnYKoRhOoam4oHFernOLOkbJKzzC/
5xzBqTIGx6SbmlcrPFPDjcJ8fn8CThVo
-----END PUBLIC KEY-----
    """.trimIndent()

    val affineX = "0ks_RiTQ82tMW4UEQu0LLDqqEEcj6yceZF5YuUU-IqOKaMAu4_tsbyE-hM4WDjZY"
    val affineY = "G6cSnYKoRhOoam4oHFernOLOkbJKzzC_5xzBqTIGx6SbmlcrPFPDjcJ8fn8CThVo"
    val ecPublicKeyStr = """
    {
         "kid": "1234",
         "kty":"EC",
         "crv":"P-384",
         "x":"$affineX",
         "y":"$affineY"
    }
    """.trimIndent()

    val ecPrivateKey = """
    {
         "kid": "1234",
         "kty":"EC",
         "d":"ty7q_3nZCTaY80-69YHJ18cN2vC1lRIuULMeKvhpg6C24fxCw_vHjHlF80EzcTX7",
         "crv":"P-384",
         "x":"$affineX",
         "y":"$affineY"
    }
    """.trimIndent()

    val modulus = "xJRuufBk_axjyO1Kpy5uwmnAY0VUhCzG8G4OiDVgnaXeLMzj91bcQdYOMQ_82PTGrUbck3qSFXbug_Ljj8NZDT0J1ZSKv8Oce-GdkeNzA5W9yvChvorGotAUWMS7_EXXxz8mjlrwu6kyKfCpuJAMg5VrZaYA0nAlv-e7zoRE9pQ0VHNrEaj6Uikw3M02oVHUNiRtL5Y5tYyz_yRBauVLPdHf5Yf-cZeO2x02qFSGcl_2EzWZcGb6PkQZ_9QeOq_iJ9h-NU_wb9lnbebnBhPGAyc1-_9vnFlFzkH2lt0BVtfhW0E4ieKkntbC0QFxNu91Gf4jfFmsOAsCf3UpVqWIQw"
    val exponent = "AQAB"
    val rsaPublicKeyStr = """
    {
        "kty":"RSA",
        "kid":"11209921-860e-4b6d-8d7e-adc8778e1c6c",
        "n": "$modulus",
        "e": "$exponent"
    }
    """.trimIndent()

    val jwkString = """
              { "kty": "ES",
                "use": "sig",
                "kid": "ignore.REDOX",
                 "x5c": [ "a", "b", "c" ]
              }
        """.trimIndent()

    val jwk = Jwk(kty ="ES", x = "x", y = "y", crv = "crv", kid = "myId", x5c = listOf("a", "b"))

    // this jwkset is from the RFC specification for JWKs, so I thought it would be a nice test.
    // See https://tools.ietf.org/html/rfc7517
    val niceExampleJwkSetString = """
        { 
         "scope": "foobar",
         "keys":
         [
         {"kty":"EC",
          "crv":"P-256",
          "x":"MKBCTNIcKUSDii11ySs3526iDZ8AiTo7Tu6KPAqv7D4",
          "y":"4Etl6SRW2YiLUrN5vfvVHuhp7x8PxltmWWlbbM4IFyM",
          "use":"enc",
          "kid":"1"},

         {"kty":"RSA",
          "n": "0vx7agoebGcQSuuPiLJXZptN9nndrQmbXEps2aiAFbWhM78LhWx4cbbfAAtVT86zwu1RK7aPFFxuhDR1L6tSoc_BJECPebWKRXjBZCiFV4n3oknjhMstn64tZ_2W-5JsGY4Hc5n9yBXArwl93lqt7_RN5w6Cf0h4QyQ5v-65YGjQR0_FDW2QvzqY368QQMicAtaSqzs8KJZgnYb9c7d0zgdAZHzu6qMQvRL5hajrn1n91CbOpbISD08qNLyrdkt-bFTWhAI4vMQFh6WeZu0fM4lFd2NcRwr3XPksINHaQ-G_xBniIqbw0Ls1jF44-csFCur-kEgU8awapJzKnqDKgw",          "e":"AQAB",
          "alg":"RS256",
          "kid":"2011-04-29"}
         ]
         }
        """.trimIndent()

    @Test
    fun `test convert json string to Jwk obj`() {
        val obj: Jwk = jacksonObjectMapper().readValue(jwkString, Jwk::class.java)
        assertNotNull(obj)
        assertEquals("ES", obj.kty)
        assertEquals("ignore.REDOX", obj.kid)
        assertEquals(3, obj.x5c?.size)
        assertEquals("c", obj.x5c?.get(2))
    }

    @Test
    fun `test covert Jwk obj to json string`() {
        val json = jacksonObjectMapper().writeValueAsString(jwk)
        assertNotNull(json)
        val tree = jacksonObjectMapper().readTree(json)
        assertEquals("ES", tree["kty"].textValue())
        assertEquals("x", tree["x"].textValue())
        assertEquals("b", (tree["x5c"] as ArrayNode)[1].textValue())
    }

    @Test
    fun `test convert JSON String to RSAPublicKey`() {
        val rsaPublicKey = Jwk.generateRSAPublicKey(rsaPublicKeyStr)
        assertNotNull(rsaPublicKey)
        assertEquals("RSA",rsaPublicKey.algorithm)
        assertNotNull(rsaPublicKey)  // lame test
        assertEquals(BigInteger(exponent.decodeBase64()), rsaPublicKey.publicExponent)
        // not so straightforward as this
        // assertEquals(BigInteger(modulus.decodeBase64()), rsaPublicKey.modulus)
    }

    @Test
    fun `test convert JSON String to ECPublicKey`() {
        val ecPublicKey = Jwk.generateECPublicKey(ecPublicKeyStr)
        assertNotNull(ecPublicKey)
        assertNotNull(ecPublicKey)
        assertEquals("EC",ecPublicKey.algorithm)
        assertNotNull(ecPublicKey.w)  // lame test
        // not so straightforward as this
        // assertEquals(BigInteger(affineX.decodeBase64()), ecPublicKey.w.affineX)
    }

    @Test
    fun `test convert pem to ECPublicKey`() {
        // Step 1:   convert the .pem file into a nimbusds JWK obj.
        val nimbusdsJwk = JWK.parseFromPEMEncodedObjects(pemPublicESKey)
        assertEquals(KeyType.EC, nimbusdsJwk.keyType)
        // Steps 2,3:  convert the JWK obj to a string, then use that to generate the public key obj
        val ecPublicKey = Jwk.generateECPublicKey(nimbusdsJwk.toJSONString())
        assertNotNull(ecPublicKey)
        assertEquals("EC",ecPublicKey.algorithm)
        assertNotNull(ecPublicKey.w)  // lame test
    }

    @Test
    fun `test converting nice example JwkSet string to obj`() {
        val jwkSet = jacksonObjectMapper().readValue(niceExampleJwkSetString, JwkSet::class.java)
        assertEquals("foobar", jwkSet.scope)
        assertEquals(2, jwkSet.keys.size)
        val key1 = jwkSet.keys[0]
        assertEquals("EC", key1.kty)
        assertEquals("enc", key1.use)
        assertEquals("1", key1.kid)
        val key2 = jwkSet.keys[1]
        assertEquals("RSA", key2.kty)
        assertEquals("RS256", key2.alg)
        assertEquals("2011-04-29", key2.kid)
    }

    @Test
    fun `test converting nice example JwkSet string to RSA and EC public keys`() {
        val jwkSet = jacksonObjectMapper().readValue(niceExampleJwkSetString, JwkSet::class.java)
        assertEquals("foobar", jwkSet.scope)
        assertEquals(2, jwkSet.keys.size)
        val ecPublicKey = jwkSet.keys[0].toECPublicKey()
        assertNotNull(ecPublicKey)
        assertEquals("EC",ecPublicKey.algorithm)
        assertNotNull(ecPublicKey.w)  // lame test
        val rsaPublicKey = jwkSet.keys[1].toRSAPublicKey()
        assertNotNull(rsaPublicKey)
        assertEquals("RSA",rsaPublicKey.algorithm)
        assertNotNull(rsaPublicKey)  // lame test
        assertEquals(BigInteger(exponent.decodeBase64()), rsaPublicKey.publicExponent)

    }

    @Test
    fun `test convert string FHIRAuth to ECPublicKey`() {
        val jwkSetString = """
            {  
                "scope": "read:reports",
                "keys": [  $ecPublicKeyStr ]
            }
        """.trimIndent()
        val jwkSet = jacksonObjectMapper().readValue(jwkSetString, JwkSet::class.java)
        val ecPublicKey = jwkSet.keys[0].toECPublicKey()
        assertNotNull(ecPublicKey)
        assertEquals("EC",ecPublicKey.algorithm)
        assertNotNull(ecPublicKey.w)  // lame test
    }
}