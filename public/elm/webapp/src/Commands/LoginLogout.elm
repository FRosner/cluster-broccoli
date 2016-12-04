module Commands.LoginLogout exposing (loginRequest)

import Commands.Fetch exposing (apiBaseUrl)
import Json.Decode
import Http
import Updates.Messages exposing (UpdateLoginStatusMsg(..))
import Models.Resources.UserInfo exposing (userInfoDecoder)

authBaseUrl = String.concat [ apiBaseUrl, "/auth" ]
loginUrl = String.concat [ authBaseUrl, "/login" ]
logoutUrl = String.concat [ authBaseUrl, "/logout" ]

requestBody username password =
  Http.multipartBody
    [ Http.stringPart "username" username
    , Http.stringPart "password" password
    ]

loginRequest : String -> String -> Cmd UpdateLoginStatusMsg
loginRequest username password =
  Http.post loginUrl (requestBody username password) userInfoDecoder
    |> Http.send FetchLogin
