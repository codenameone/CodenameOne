print("init a", __name__)
counter = 0
def twice(x):
    global counter
    counter += 1
    return x * 2
