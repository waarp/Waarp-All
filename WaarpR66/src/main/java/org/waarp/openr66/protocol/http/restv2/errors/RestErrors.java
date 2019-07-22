package org.waarp.openr66.protocol.http.restv2.errors;

/**
 * Factory class used to create instances of {@link RestError} representing
 * input errors in an HTTP request made to the REST API.
 */
public final class RestErrors {

    /** Prevent the default constructor from being called. */
    private RestErrors() throws InstantiationException {
        throw new InstantiationException(this.getClass().getName() +
                " cannot be instantiated.");
    }

    /**
     * Creates an error saying that the request is missing a body when one
     * was required.
     *
     * @return the corresponding new RestError object
     */
    public static RestError MISSING_BODY() {
        return new RestError("BadRequest.MissingBody", new String[0], 1);
    }

    /**
     * Creates an error saying that the request content is not a valid JSON object.
     *
     * @return the corresponding new RestError object
     */
    public static RestError MALFORMED_JSON(int line, int column, String cause) {
        String[] args = {Integer.toString(line), Integer.toString(column), cause};
        return new RestError("BadRequest.MalformedJSON", args, 2);
    }

    /**
     * Creates an error saying that JSON object given has a duplicate field.
     *
     * @param field the missing parameter's name
     * @return      the corresponding new RestError object
     */
    public static RestError DUPLICATE_KEY(String field) {
        String[] args = {field};
        return new RestError("BadRequest.DuplicateKey", args, 3);
    }

    /**
     * Creates an error saying that one of the request's parameters has an
     * illegal value. This includes numbers out of their expected range, or
     * invalid enum values.
     *
     * @param parameter the incorrect parameter's name
     * @return          the corresponding new RestError object
     */
    public static RestError ILLEGAL_PARAMETER_VALUE(String parameter, String value) {
        String[] args = {value, parameter};
        return new RestError("BadRequest.IllegalParameterValue", args, 4);
    }

    /**
     * Creates an error saying that the JSON object sent had an extra unknown
     * field named with the given name.
     *
     * @param field the extra field encountered
     * @return      the corresponding new RestError object
     */
    public static RestError UNKNOWN_FIELD(String field) {
        String[] args = {field};
        return new RestError("BadRequest.UnknownField", args, 5);
    }

    /**
     * Creates an error saying that one of the JSON object's field is missing
     * when it was required. This error can also be sent if the field is present
     * but is missing its' value.
     *
     * @param field the field whose value is missing
     * @return      the corresponding new RestError object
     */
    public static RestError MISSING_FIELD(String field) {
        String[] args = {field};
        return new RestError("BadRequest.MissingFieldValue", args, 6);
    }

    /**
     * Creates an error saying that the given field has been assigned an illegal
     * value. This includes values of the wrong type, or values outside the
     * allowed bounds of the field.
     *
     * @param field the name of the field whose value is incorrect
     * @param value the textual representation of the incorrect value
     * @return      the corresponding new RestError object
     */
    public static RestError ILLEGAL_FIELD_VALUE(String field, String value) {
        String[] args = {value, field};
        return new RestError("BadRequest.IllegalFieldValue", args, 7);
    }

    /**
     * Creates an error saying that the database already contains an entry with
     * the same ID as the one in the entry the user tried to create.
     * Since the database cannot contain entries with duplicate IDs, the
     * requested entry cannot be created.
     *
     * @param id the duplicate ID in the requested collection
     * @return   the corresponding new RestError object
     */
    public static RestError ALREADY_EXISTING(String id) {
        String[] args = {id};
        return new RestError("BadRequest.AlreadyExisting", args, 8);
    }

    /**
     * Creates an error saying that the file requested for import at the given
     * location does not exist on the server.
     *
     * @param path the incorrect path
     * @return     the corresponding new RestError object
     */
    public static RestError FILE_NOT_FOUND(String path) {
        String[] args = {path};
        return new RestError("BadRequest.FileNotFound", args, 9);
    }

    /**
     * Creates an error saying that the transfer rule entered alongside the
     * newly created transfer does not exist.
     *
     * @param rule the unknown rule's name
     * @return     the corresponding new RestError object
     */
    public static RestError UNKNOWN_RULE(String rule) {
        String[] args = {rule};
        return new RestError("BadRequest.UnknownRule", args, 10);
    }

    /**
     * Creates an error saying that the requested host name entered alongside
     * the newly created transfer does not exist.
     *
     * @param host the unknown host name
     * @return     the corresponding new RestError object
     */
    public static RestError UNKNOWN_HOST(String host) {
        String[] args = {host};
        return new RestError("BadRequest.UnknownHost", args, 11);
    }

    /**
     * Creates an error saying that the given host is not allowed to use the
     * given rule for its transfers.
     *
     * @param host the host processing the transfer
     * @param rule the rule which the host is not allowed to use
     * @return     the corresponding new RestError object
     */
    public static RestError RULE_NOT_ALLOWED(String host, String rule) {
        String[] args = {host, rule};
        return new RestError("BadRequest.RuleNotAllowed", args, 12);
    }

    /**
     * Creates an error saying that the given field is a primary key, and thus
     * cannot be changed when updating an entry.
     *
     * @param field the name of the field which was illegally modified
     * @return      the corresponding new RestError object
     */
    public static RestError FIELD_NOT_ALLOWED(String field) {
        String[] args = {field};
        return new RestError("BadRequest.UnauthorizedField", args, 13);
    }
}
