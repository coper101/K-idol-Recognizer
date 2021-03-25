# Files imported into python directory
# - labels.pickle
# - encodings.pickle

import os
import numpy as np
import pickle
import cv2
import base64
import face_recognition
import pandas as pd


def detect_face_fr(image_data):
    # Decode Image Data and Convert To Numpy
    decode_data = base64.b64decode(image_data)
    np_data = np.fromstring(decode_data, np.uint8)
    image_4_channels = cv2.imdecode(np_data, cv2.IMREAD_UNCHANGED)
    img = image_4_channels[...,:3]

    labels, encodings = retrieve_encodings_labels()

    # Using Face Recognition
    imgSmall = cv2.resize(img, (0, 0), None, 0.25, 0.25)
    imgSmall = cv2.cvtColor(imgSmall, cv2.COLOR_BGR2RGB)

    facesCurFrame = face_recognition.face_locations(imgSmall)
    encodesCurFrame = face_recognition.face_encodings(imgSmall, facesCurFrame)

    # Loop Faces for Current Frame
    idsBbox = []
    for encodeFace, faceLoc in zip(encodesCurFrame, facesCurFrame):
        matches = face_recognition.compare_faces(encodings, encodeFace)
        faceDis = face_recognition.face_distance(encodings, encodeFace)
        matchIndex = np.argmin(faceDis)

        if matches[matchIndex]:
            # (1) label
            temp = []
            label = labels[matchIndex]
            temp.append(extractId(label))
            # (2) bounding box axis
            y1, x2, y2, x1 = faceLoc
            y1, x2, y2, x1 = y1*4, x2*4, y2*4, x1*4
            temp.extend([x1, x2, y1, y2])
            # add to nested list
            idsBbox.append(temp)

    return idsBbox

def extractId(label):
    id = label.split('_')[0]
    return id

def retrieve_encodings_labels():

    lblFileName = os.path.join(os.path.dirname(__file__), "labels.pickle")
    lblFile = open(lblFileName, "rb")
    labels = pickle.load(lblFile)
    lblFile.close()

    encFileName = os.path.join(os.path.dirname(__file__), "encodings.pickle")
    encFile = open(encFileName, "rb")
    encodings = pickle.load(encFile)
    encFile.close()

    return labels, encodings

def get_idol_profile(id):

    dataFileName = os.path.join(os.path.dirname(__file__), "Kpop_Idols_210321_CSV.csv")
    data = pd.read_csv(dataFileName)

    fltr = data['Id'] == int(id)
    idol = data.loc[fltr]

    headerVal = data.columns.values.tolist()
    try:
        idolVal = idol.values[0].tolist()
    except:
        idolVal = []

    idolProfile = {}

    if len(headerVal) > 0 and len(idolVal) > 0:
        for i in range(len(headerVal)):
            idolProfile[headerVal[i]] = idolVal[i]

    return idolProfile


def getType(value):
    return type(value)



