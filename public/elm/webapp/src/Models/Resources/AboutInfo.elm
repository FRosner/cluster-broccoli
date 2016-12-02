module Models.Resources.AboutInfo exposing (..)

type alias ProjectInfo =
  { name : String
  , version : String
  }

type alias ScalaInfo =
  { version : String
  }

type alias SbtInfo =
  { version : String
  }

type alias UserInfo =
  { name : String
  , role : String
  , instanceRegex : String
  }

type alias AuthInfo =
  { enabled : Bool
  , userInfo : UserInfo
  }

type alias AboutInfo =
    { projectInfo : ProjectInfo
    , scalaInfo : ScalaInfo
    , sbtInfo : SbtInfo
    , authInfo : AuthInfo
    }
