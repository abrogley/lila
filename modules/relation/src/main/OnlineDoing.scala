package lila.relation

import com.github.blemale.scaffeine.{ Cache, Scaffeine }
import scala.concurrent.duration._

import actorApi.OnlineFriends
import lila.user.User

final class OnlineDoing(
    api: RelationApi,
    lightUser: lila.common.LightUser.GetterSync,
    val userIds: lila.memo.ExpireSetMemo
) {

  private type StudyId = String

  val playing = new lila.memo.ExpireSetMemo(4 hours)

  // people with write access in public studies
  val studying: Cache[ID, StudyId] =
    Scaffeine().expireAfterAccess(20 minutes).build[ID, StudyId]

  // people with write or read access in public and private studies
  val studyingAll: Cache[ID, StudyId] =
    Scaffeine().expireAfterAccess(20 minutes).build[ID, StudyId]

  def isStudying(userId: User.ID) = studying.getIfPresent(userId).isDefined

  def isStudying(userId: User.ID, studyId: StudyId) = studying.getIfPresent(userId) has studyId

  def friendsOf(userId: User.ID): Fu[OnlineFriends] =
    api fetchFollowing userId map userIds.intersect map { friends =>
      OnlineFriends(
        users = friends.toList.flatMap { lightUser(_) },
        playing = playing intersect friends,
        studying = friends filter studying.getAllPresent(friends).contains
      )
    }
}
