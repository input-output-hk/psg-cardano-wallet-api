package iog.psg.cardano.experimental.cli.model

import org.apache.commons.codec.binary.Base16

import java.nio.charset.StandardCharsets

case class Base16String private (value: String) extends AnyVal

object Base16String {
  private val base16 = new Base16(true)

  def apply(value: String): Base16String =
    new Base16String(base16.encodeAsString(value.getBytes(StandardCharsets.UTF_8)))

  def validate(value: String): Option[Base16String] =
    Option.when(base16.isInAlphabet(value))(new Base16String(value))
}

