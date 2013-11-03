package org.fedoraproject.mobile.util

import java.security.MessageDigest

object Hashing {
  def md5(s: String): String =
    MessageDigest
      .getInstance("MD5")
      .digest(s.getBytes)
      .map("%02x".format(_))
      .mkString
}
