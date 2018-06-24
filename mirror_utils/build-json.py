import os
import json
import hashlib
import gzip
import shutil
import tempfile
import pathlib

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
    with open(file, 'rb') as afile:
        return get_sha1_hash_from_filehandle(afile)


def get_sha1_hash_from_filehandle(filehandle):
    hasher = hashlib.sha1()

    BLOCKSIZE = 65536
    buf = filehandle.read(BLOCKSIZE)
    while len(buf) > 0:
        hasher.update(buf)
        buf = filehandle.read(BLOCKSIZE)

    return hasher.hexdigest()


def get_hash_of_file_contents(full_path):
    if ".gz" not in pathlib.Path(full_path).suffix:
        return get_sha1_hash(full_path)

    # need to hash the *unzipped* contents of the file, so, extract it to a temp file and hash
    # the uncompressed data in it.
    with gzip.open(full_path, 'rb') as f_zipped_in:
        with tempfile.TemporaryFile() as f_unzipped_out:
            shutil.copyfileobj(f_zipped_in, f_unzipped_out)
            f_unzipped_out.seek(0)
            return get_sha1_hash_from_filehandle(f_unzipped_out)


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

            full_path = basedir+dir+'/'+file

            out[dir][key_name] = get_hash_of_file_contents(full_path)

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
