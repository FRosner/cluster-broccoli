port module Main exposing (..)

import Utils.ParameterUtilsSuite as ParameterUtilsSuite
import Utils.DictUtilsSuite as DictUtilsSuite
import Views.HeaderSuite as HeaderSuite
import Views.NotificationsSuite as NotificationsSuite
import Views.BodySuite as BodySuite
import Views.FooterSuite as FooterSuite
import Views.PeriodicRunsViewSuite as PeriodicRunsViewSuite
import Views.LogUrlSuite as LogUrlSuite
import Test exposing (describe)
import Test.Runner.Node exposing (run, TestProgram)
import Json.Encode exposing (Value)


main : TestProgram
main =
    run
        emit
        (describe
            "Cluster Broccoli UI Tests"
            [ ParameterUtilsSuite.tests
            , DictUtilsSuite.tests
            , HeaderSuite.tests
            , NotificationsSuite.tests
            , BodySuite.tests
            , FooterSuite.tests
            , LogUrlSuite.tests
            , PeriodicRunsViewSuite.tests
            ]
        )


port emit : ( String, Value ) -> Cmd msg
