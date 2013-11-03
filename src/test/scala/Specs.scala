import org.fedoraproject.mobile.util.Hashing

import org.scalacheck.Properties
import org.scalacheck.Prop.forAll

object StringSpecification extends Properties("String") {
  property("length") = forAll { (a: String) =>
    Hashing.md5(a).length == 32
  }

  property("hexadecimal") = forAll { (a: String) =>
    Hashing.md5(a).forall((('0' to '9') ++ ('a' to 'f')).contains(_))
  }
}
