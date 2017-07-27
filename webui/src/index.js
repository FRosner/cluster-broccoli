import './favicon-300.png';

// Load all style sheets
import 'font-awesome/css/font-awesome.css';
import 'bootstrap/dist/css/bootstrap.css';
import 'animate.css/animate.css';
import './index.css';

import 'jquery';
import 'bootstrap/dist/js/bootstrap';

// Import the application from ELM
import * as Elm from './Main.elm';

// Export this function on window to make it available for Elm
window.copy = (text) => {
    // I was not able to do this in Elm so I had to use the JS function here
    // Elm ports also don't work because at least Chrome needs a proper onclick function for execCommand('copy') to work
    var dummy = document.createElement("input");
    document.body.appendChild(dummy);
    dummy.setAttribute("id", "dummy_id");
    document.getElementById("dummy_id").value = text;
    dummy.select();
    document.execCommand("copy");
    document.body.removeChild(dummy);
};

export const app = Elm.Main.embed(document.getElementById('main'));
