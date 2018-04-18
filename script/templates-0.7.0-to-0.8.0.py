import re
import json
import glob
import argparse
import os
from copy import deepcopy
from pyhocon import HOCONConverter, ConfigFactory

def get_template_variables(template):
    variablePattern = re.compile("\\{\\{([A-Za-z][A-Za-z0-9\\-\\_\\_]*)\\}\\}")
    variables = set(variablePattern.findall(template))
    return variables


def update_parameter_infos(parameter_infos, template_variables, template_path):
    updated_parameter_infos = deepcopy(parameter_infos)
    info_variables = set(parameter_infos.keys())
    for variable in info_variables.union(template_variables):
        if variable not in info_variables:
            print("Adding missing variable '%s' to the parameters of template %s" % (variable, template_path))
            updated_parameter_infos[variable] = {"id": variable}
        if "type" not in updated_parameter_infos[variable]:
            print("Adding missing type for '%s' to the parameters of template %s" % (variable, template_path))
            updated_parameter_infos[variable]["type"] = "raw"
    return updated_parameter_infos


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("template_directory")

    opts = parser.parse_args()

    for path in glob.glob(os.path.join(opts.template_directory, "*/*.conf")):
        print("Processing %s" % path)
        # template_conf = ConfigFactory.parse_file(path)
        template_conf = json.loads(open(path).read())
        parameter_infos = template_conf["parameters"]
        template = open(path[:-5] + ".json").read()
        template_variables = get_template_variables(template)
        complete_parameter_infos = update_parameter_infos(parameter_infos, template_variables, path)

        parameter_infos_changed = False
        if parameter_infos != complete_parameter_infos:
            parameter_infos_changed = True
            template_conf["parameters"] = complete_parameter_infos

        if parameter_infos_changed:
            print("Overwritting template: %s" % path)
            open(path, "w+")\
                .write(
                    json.dumps(
                        template_conf, indent=2, separators=(',', ': ', ), ensure_ascii=False) + "\n"
                )
