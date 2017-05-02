module Models.Ui.LoginForm exposing (LoginForm, empty)

type alias LoginForm =
  { username : String
  , password : String
  , loginIncorrect : Bool
  }

empty = LoginForm "" "" False
