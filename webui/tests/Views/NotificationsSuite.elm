module Views.NotificationsSuite exposing (tests)

import Expect as Expect
import Messages exposing (AnyMsg(UpdateErrorsMsg))
import Test exposing (Test, describe, test)
import Test.Html.Events as Events
import Test.Html.Query as Query
import Test.Html.Selector as Selector
import Updates.Messages exposing (UpdateErrorsMsg(CloseError))
import Views.Notifications as Notifications


tests : Test
tests =
    describe "Notifications View"
        [ test "Should render nothing if there are no errors" <|
            \() ->
                let
                    errors =
                        []
                in
                Notifications.view errors
                    |> Query.fromHtml
                    |> Query.hasNot [ Selector.class "alert" ]
        , test "Should render errors correctly" <|
            \() ->
                let
                    errors =
                        [ "1", "2" ]
                in
                Notifications.view errors
                    |> Query.fromHtml
                    |> Query.findAll [ Selector.class "alert" ]
                    |> Query.count (Expect.equal (List.length errors))
        , test "Should close errors correctly" <|
            \() ->
                let
                    errors =
                        [ "1", "2" ]
                in
                Notifications.view errors
                    |> Query.fromHtml
                    |> Query.find [ Selector.id "close-error-1" ]
                    |> Events.simulate Events.Click
                    |> Events.expectEvent (UpdateErrorsMsg (CloseError 1))
        ]
