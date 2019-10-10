package controllers

import java.io.File
import java.nio.file.Paths
import java.util.UUID

import forms.Forms._
import javax.inject.Inject
import models._
import org.apache.commons.io.FilenameUtils
import play.api.Configuration
import play.api.i18n.I18nSupport
import play.api.inject.ApplicationLifecycle
import play.api.mvc.{Action, _}

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._

class AdminController @Inject()
  (implicit ec: ExecutionContext, config: Configuration, lifecycle: ApplicationLifecycle, val controllerComponents: ControllerComponents,
    workRepo: WorkRepo, imageRepo: ImageRepo, userRepo: UserRepo, sessionRepo: SessionRepo
  )

  extends BaseController with I18nSupport {

  private def checkSession(option: Option[String]): Boolean = {
    option match {
      case Some(sessionKey) => sessionRepo.keyExists(sessionKey)
      case None => false
    }
  }

  def index: Action[AnyContent] = Action { implicit request =>
    checkSession(request.session.get("gallery-session")) match {
      case true => Ok(views.html.admin.index());
      case false => InternalServerError("No Valid Session Key")
    }
  }


  def createWork: Action[AnyContent] = Action { implicit request =>
    checkSession(request.session.get("gallery-session")) match {
      case true => Ok(views.html.admin.createWork(workForm))
      case false => InternalServerError("No Valid Session Key")
    }
  }


  def insertWork: Action[AnyContent] = Action { implicit request =>
    checkSession(request.session.get("gallery-session")) match {
      case true => {
        workForm.bindFromRequest.fold(
          formWithErrors => {
            Redirect(routes.AdminController.createWork()) //add error to flash
          },
          form => {
            val work = Work(form.id, form.title, form.description, form.creationDate, form.available)
            val workId = Await.result(workRepo.create(work), 5.seconds).id
            Redirect(routes.AdminController.uploadImage(workId))
          }
        )
      }
      case false => InternalServerError("No Valid Session Key")
    }
  }

  def uploadImage(workId: Int): Action[AnyContent] = Action { implicit request =>
    checkSession(request.session.get("gallery-session")) match {
      case true => {
        Await.result(workRepo.findById(workId), 5.seconds) match {
          case Some(work) => Ok(views.html.admin.upload(work))
          case None => InternalServerError("InternalServerError")
        }
      }
      case false => InternalServerError("Invalid Key")
    }
  }

  def upload = Action(parse.multipartFormData) { request =>
    val workId = request.body.asFormUrlEncoded("work_id")(0).toInt

    request.body
      .file("picture")
      .map { picture =>
        val ext = FilenameUtils.getExtension(Paths.get(picture.filename).getFileName.toString)
        val uuid = UUID.randomUUID().toString
        val loc = new File(s"public/images/gallery/$uuid.$ext")
        picture.ref.moveTo(Paths.get(loc.getCanonicalPath), replace = true)
        val image = new Image(0, workId, 1, uuid, ext)
        Await.result(imageRepo.create(image), 5.seconds)
        Redirect(routes.AppController.show(workId))
      }
      .getOrElse { InternalServerError("Internal Server Error") }
  }

  def insertAdmin: Action[AnyContent] = Action { implicit request =>
    userForm.bindFromRequest.fold(
      formWithErrors => {
        Redirect(routes.AppController.index())
      },
      form => {
        val user = Await.result(userRepo.create(form), 5.seconds)
        Redirect(routes.AdminController.login())
      }
    )
  }

  def insertUser: Action[AnyContent] = Action { implicit request =>
    request.session.get("gallery-session").map { sessionKey =>
      sessionRepo.keyExists(sessionKey) match {
        case true => {
          userForm.bindFromRequest.fold(
            formWithErrors => {
              Redirect(routes.AppController.index())
            },
            form => {
              val user = Await.result(userRepo.create(form), 5.seconds)
              Redirect(routes.AdminController.index())
            }
          )
        }
        case false => InternalServerError("Invalid Key")
      }
    }.getOrElse {
      InternalServerError("No Session Key")
    }
  }

  def login() = Action { implicit request =>
    userRepo.adminExists() match {
      case true => Ok(views.html.admin.login(authForm))
      case false => Ok(views.html.admin.setup(userForm))
    }
  }

}

