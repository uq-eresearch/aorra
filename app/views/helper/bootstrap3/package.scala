package views.html.helper

package object bootstrap3 {

  implicit val bootstrapField = new FieldConstructor {
    def apply(elts: FieldElements) = bootstrapFieldConstructor(elts)
  }

}