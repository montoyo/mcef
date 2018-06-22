import os
import json
import hashlib

# Quick script to build new config2.json for mirrors for new releases of CEF/JCEF
# In the same folder as this script, make a folder called "dist/" and put the binaries there.
# Use the same structure as the existing mirror here: http://montoyo.net/jcef/
#
# For testing, override the mirror URL in MCEF, and run the following command to start a local web server:
#   cd dist/
#   py -m http.server 8000
#
# -Dom

# version number we want to build (usually increment this for new releases)
mcef_version = "0.10"

# copy of config2.json from previous release
# (download a live copy from http://montoyo.net/jcef/config2.json and rename to this)
inputjson = "baseconfig-0.9.json"
minecraft_version = "1.12.2"

# -------------------------------------------------
# shouldn't need to modify anything else below this
# -------------------------------------------------

topdir = "dist/"
out = topdir+"config2.json"


def get_sha1_hash(file):
    BLOCKSIZE = 65536
    hasher = hashlib.sha1()
    with open(file, 'rb') as afile:
        buf = afile.read(BLOCKSIZE)
        while len(buf) > 0:
            hasher.update(buf)
            buf = afile.read(BLOCKSIZE)

    return hasher.hexdigest()


def create_data_from_files(basedir):
    out = {}
    dist_dirs = [x[1] for x in os.walk(basedir)]

    dirs_to_search = dist_dirs[0]
    print(dirs_to_search)

    for dir in dirs_to_search:
        out[dir] = {}
        print("----" + dir + "----")
        for file in os.listdir(basedir+dir):
            print("  " + file)
            key_name = ("@" if ".gz" in file else "") + file
            out[dir][key_name] = get_sha1_hash(basedir+dir+'/'+file)

    return out


basedir = topdir + mcef_version + '/'

with open(inputjson) as f:
    config = json.load(f)

config[mcef_version] = {}
config[mcef_version]["platforms"] = create_data_from_files(basedir)
config[mcef_version]["extract"] = ["locales.zip"]
config["latestVersions"][minecraft_version] = mcef_version

with open(out, 'w') as outfile:
    json.dump(config, outfile, indent=4, separators=(',', ': '), sort_keys=True)
