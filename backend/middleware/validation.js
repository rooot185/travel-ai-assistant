const Joi = require('joi');

const loginSchema = Joi.object({
  username: Joi.string().alphanum().min(3).max(30).required(),
  password: Joi.string().min(6).required()
});

const registerSchema = Joi.object({
  username: Joi.string().alphanum().min(3).max(30).required(),
  email: Joi.string().email().required(),
  password: Joi.string().min(6).required()
});

const travelPlanSchema = Joi.object({
  destination: Joi.string().min(1).max(100).required(),
  startDate: Joi.string().isoDate().required(),
  days: Joi.number().integer().min(1).max(30).required(),
  travelers: Joi.number().integer().min(1).max(20).required(),
  budget: Joi.number().min(0).required(),
  preferences: Joi.array().items(Joi.string()).default([]),
  additionalRequirements: Joi.string().max(1000).default('')
});

const validateRequest = (schema) => {
  return (req, res, next) => {
    const { error, value } = schema.validate(req.body);

    if (error) {
      return res.status(400).json({
        error: 'Validation Error',
        details: error.details.map(detail => ({
          field: detail.path.join('.'),
          message: detail.message
        }))
      });
    }

    req.body = value;
    next();
  };
};

module.exports = {
  validateLogin: validateRequest(loginSchema),
  validateRegister: validateRequest(registerSchema),
  validateTravelPlan: validateRequest(travelPlanSchema),
  schemas: {
    loginSchema,
    registerSchema,
    travelPlanSchema
  }
};