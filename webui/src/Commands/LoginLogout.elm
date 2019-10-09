module Commands.LoginLogout exposing (loginRequest, logoutRequest, verifyLogin)

import Commands.Fetch exposing (apiBaseUrl)
import Http
import Json.Decode
import Models.Resources.UserInfo exposing (userInfoDecoder)
import Updates.Messages exposing (UpdateLoginStatusMsg(..))


authBaseUrl =
    String.concat [ apiBaseUrl, "/auth" ]


loginUrl =
    String.concat [ authBaseUrl, "/login" ]


logoutUrl =
    String.concat [ authBaseUrl, "/logout" ]


verifyUrl =
    String.concat [ authBaseUrl, "/verify" ]


requestBody username password =
    Http.multipartBody
        [ Http.stringPart "username" username
        , Http.stringPart "password" password
        ]


loginRequest : String -> String -> Cmd UpdateLoginStatusMsg
loginRequest username password =
    Http.post loginUrl (requestBody username password) userInfoDecoder
        |> Http.send FetchLogin


logoutRequest : Cmd UpdateLoginStatusMsg
logoutRequest =
    Http.post logoutUrl Http.emptyBody Json.Decode.string
        |> Http.send FetchLogout


verifyLogin : Cmd UpdateLoginStatusMsg
verifyLogin =
    Http.getString verifyUrl
        |> Http.send FetchVerify
