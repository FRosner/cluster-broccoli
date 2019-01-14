import os
import argparse
import json
from copy import deepcopy
from pyhocon import ConfigFactory, HOCONConverter


def update_parameters(parameters):
    updated_parameters = deepcopy(parameters)
    info_variables = set(updated_parameters.keys())
    for variable in info_variables:
        if type(updated_parameters[variable]['type']) == str:
            old_type = updated_parameters[variable]['type']
            updated_parameters[variable]['type'] = ConfigFactory.from_dict({"name": old_type})
        else:
            print("not changing type for " + variable)
    return updated_parameters


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("template_directory")
    parser.add_argument("output_format", choices=["json", "hocon"])

    opts = parser.parse_args()

    for directory_name in os.listdir(os.path.join(opts.template_directory)):
        if not os.path.isdir(os.path.join(opts.template_directory, directory_name)):
            print(directory_name + " was not a directory. Skipping.")
            continue

        directory_path = os.path.join(opts.template_directory, directory_name)
        template_path = os.path.join(directory_path, "template.json")
        config_path = os.path.join(directory_path, "template.conf")
        if not os.path.isfile(template_path):
            print("%s is not a template directory (template file is missing)" % directory_path)
            continue

        if not os.path.isfile(config_path):
            print("%s is not a template directory (config file is missing)" % directory_path)
            continue

        template = open(template_path).read()
        config = ConfigFactory.parse_file(config_path)
        if "parameters" not in config:
            config["parameters"] = {}

        complete_parameters = update_parameters(config["parameters"])
        parameters_changed = False
        if config["parameters"] != complete_parameters:
            parameters_changed = True
            config["parameters"] = complete_parameters

        if parameters_changed:
            print("Overwriting template.conf: %s" % config_path)
            # We can use the HOCONConverter for dumping the json as well but we have more options to control
            # the output with the default json writer
            if opts.output_format == "hocon":
                open(config_path, "w+").write(HOCONConverter.convert(config, "hocon"))
            else:
                open(config_path, "w+").write(
                    # This is a config tree but json.dumps uses iterators to go over it thinking it is a dict.
                    # Since configtree already supports these iterators it works correctly for it.
                    json.dumps(config, indent=2, separators=(',', ': ', ), ensure_ascii=False) + "\n"
                )
