// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.

// tag::python-interop-bash-001[]
mvn cn1:import-python-project -Dsource=/path/to/my-python-app
// end::python-interop-bash-001[]

// tag::python-interop-bash-002[]
jdb -sourcepath common/src/main/python -classpath ... MyAppStub
stop at com.codename1.generated.python.py_app$2eutil:6
// end::python-interop-bash-002[]

// tag::python-interop-bash-003[]
scripts/python-backend/build.sh scripts/python-backend/samples/flask-todo ./todo-server
./todo-server
// end::python-interop-bash-003[]
