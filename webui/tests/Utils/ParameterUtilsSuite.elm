module Utils.ParameterUtilsSuite exposing (tests)

import Utils.ParameterUtils exposing (getOtherParameters)
import Test exposing (test, describe, Test)
import Expect


tests : Test
tests =
    describe "ParameterUtils"
        [ describe "getOtherParameters"
            [ test "removes id parameter" <|
                \() -> Expect.equal [ "a", "b" ] (getOtherParameters [ "a", "b", "id" ])
            , test "sorts ignoring case" <|
                \() -> Expect.equal [ "bar", "FOO" ] (getOtherParameters [ "FOO", "bar" ])
            ]
        ]
