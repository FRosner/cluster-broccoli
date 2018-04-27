import os
import argparse
import json
import re
from copy import deepcopy
from pyhocon import ConfigFactory, HOCONConverter

def get_template_variables(template):
    variablePattern = re.compile("\\{\\{([A-Za-z][A-Za-z0-9\\-\\_\\_]*)\\}\\}")
    variables = set(variablePattern.findall(template))
    return variables


def update_parameters(parameters, template_variables, instance_path, param_type):
    updated_parameters = deepcopy(parameters)
    info_variables = set(updated_parameters.keys())
    for variable in info_variables.union(template_variables):
        if variable not in info_variables:
            print("Adding missing variable '%s' to the parameterInfos of instance %s" % (variable, instance_path))
            updated_parameters[variable] = ConfigFactory.from_dict({"type": param_type})
        else:
            if "type" not in updated_parameters[variable]:
                print("Adding missing type for '%s' to the parameterInfos of instance %s" % (variable, instance_path))
                updated_parameters[variable]["type"] = param_type
    return updated_parameters


def convert_dashes_to_underscores(template, parameters, template_path):
    result_template = template # This is a simple string do not need
    result_parameters = deepcopy(parameters)

    for k in parameters:
        if "-" in k:
            new_k = k.replace("-", "_")
            print("Converting variable '%s' to %s in template %s" % (k, new_k, template_path))
            assert new_k not in result_parameters
            new_v = result_parameters.pop(k)
            new_v["id"] = new_k
            result_parameters[new_k] = new_v
            result_template = result_template.replace(
                "{{" + k + "}}", "{{" + new_k + "}}"
            )

    return result_template, result_parameters


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("template_directory")
    parser.add_argument("parameter_type")
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

        template_variables = get_template_variables(template)
        complete_parameters = update_parameters(config["parameters"], template_variables,
                                                template_path, opts.parameter_type)
        parameters_changed = False
        if config["parameters"] != complete_parameters:
            parameters_changed = True
            config["parameters"] = complete_parameters

        template_without_dashes, parameters_without_dashes = convert_dashes_to_underscores(template,
                                                                                           config["parameters"],
                                                                                           template_path)

        if config['parameters'] != parameters_without_dashes:
            parameters_changed = True
            config['parameters'] = parameters_without_dashes

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

        if template_without_dashes != template:
            print("Overwriting template: %s" % template_path)
            open(template_path, "w+").write(template_without_dashes)
